-- One row per cell with four counters, 4-week blocks, per-package expiry.
--   * private.pkg_counts had one row per event type; now one row per cell
--     (package, cohort week, batch bucket, brand, ROM, ROM version, API level) with the
--     counters u (uninstalled), s (survived_reboot), r (reinstalled) and v (voided).
--   * Weeks that can no longer receive reports are merged into 4-week blocks (span = 4,
--     cohort_week = block start). What the "spread over weeks" rules need from the merged
--     weeks is kept in private.pkg_block_spread.
--   * Blocks that ended more than 52 weeks ago are deleted, but only for packages that have
--     at least 200 effective uninstalls in newer data. Rarely uninstalled packages keep their
--     history, so their verdict does not lapse.
-- Verdicts do not change because of the new table shape or because of merging.
-- Needs 20261114195541_max_packages.sql applied first (record() below is copied from it).

-- Migrations run in one transaction: block reports while the table is swapped.
lock table private.pkg_counts in access exclusive mode;

-- Fixed 4-week grid of Mondays. Immutable, so merging always lands on the same blocks.
create function private.block_of(d date) returns date
language sql immutable set search_path = '' as $$
    select date '2026-01-05' + ((d - date '2026-01-05') / 28) * 28
$$;
revoke all on function private.block_of(date) from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table private.pkg_counts_new (
    pkg          text not null,
    cohort_week  date not null,
    batch_bucket text not null,
    brand        text not null,
    rom          text not null,
    rom_version  smallint not null,
    sdk          smallint not null,
    -- 1 = one cohort week, 4 = merged block (cohort_week is the block start).
    span         smallint not null default 1,
    u            integer not null default 0,
    s            integer not null default 0,
    r            integer not null default 0,
    v            integer not null default 0,
    constraint pkg_counts_batch_bucket_check check (batch_bucket in ('1', '2-10', '11+')),
    constraint pkg_counts_span_check check (span in (1, 4)),
    constraint pkg_counts_counts_check check (s + v <= u and r <= u),
    constraint pkg_counts_new_pkey primary key (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk)
);

-- record() already keeps s + v <= u and r <= u per cell; least() only guards against an
-- old cell that would break the new check.
insert into private.pkg_counts_new
    (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u, s, r, v)
select pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk,
       u, s, least(r, u), least(v, u - s)
from (
    select pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk,
           coalesce(sum(n) filter (where event = 'uninstalled'), 0)::integer as u,
           least(coalesce(sum(n) filter (where event = 'survived_reboot'), 0),
                 coalesce(sum(n) filter (where event = 'uninstalled'), 0))::integer as s,
           coalesce(sum(n) filter (where event = 'reinstalled'), 0)::integer as r,
           coalesce(sum(n) filter (where event = 'voided'), 0)::integer as v
    from private.pkg_counts
    group by pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk
) t;

-- The score view depends on the old table, so it goes first (with the functions reading it).
drop function if exists public.package_scores();
drop function if exists public.package_scores_for(text, int);
drop materialized view if exists private.package_scores;

drop table private.pkg_counts;
alter table private.pkg_counts_new rename to pkg_counts;
alter table private.pkg_counts rename constraint pkg_counts_new_pkey to pkg_counts_pkey;

alter table private.pkg_counts enable row level security;
revoke all on private.pkg_counts from public, anon, authenticated;

-- Per package and scope ('all'/0, (rom, -1), (rom, version); the last two only for known
-- ROMs, as in the score view): how many weeks with uninstalls a block had and the largest
-- weekly count, so merging weeks does not change the "spread over weeks" rules.
create table private.pkg_block_spread (
    pkg         text not null,
    rom         text not null,
    rom_version smallint not null,
    block       date not null,
    weeks       smallint not null,
    max_week_u  integer not null,
    primary key (pkg, rom, rom_version, block)
);
alter table private.pkg_block_spread enable row level security;
revoke all on private.pkg_block_spread from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Recording events
-- ---------------------------------------------------------------------------

-- payload: exact JSON text the client hashed, e.g.
--   {"v":1,"platform":"android","device":{"brand":"xiaomi","rom":"hyperos","romVersion":1,"sdk":34},
--    "events":[{"e":"uninstalled","p":"com.foo","w":"2026-10-26","b":"11+"}]}
-- PoW: first 8 bytes (big-endian, unsigned) of
--   sha256(challenge || ':' || hex(sha256(payload)) || ':' || x)
-- must be below floor(2^64 / (pow_k * number_of_events)).
create or replace function public.record(payload text, challenge text, sig text, x bigint)
returns json
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    key        bytea;
    doc        jsonb;
    events     jsonb;
    n_events   int;
    expiry     bigint;
    h          bytea;
    h_num      numeric := 0;
    target     numeric;
    ip         text;
    salt       bytea;
    week_salt  bytea;
    v_ip_hash  bytea;
    ip_bucket  text;
    batch_pkgs text[];
    today      date := (now() at time zone 'utc')::date;
    this_week  date := date_trunc('week', now() at time zone 'utc')::date;
    min_week   date;
    plat       text;
    dev        jsonb;
    d_brand    text := 'unknown';
    d_rom      text := 'unknown';
    d_rom_ver  smallint := 0;
    d_sdk      smallint := 0;
    ev         record;
    accepted   int := 0;
    rl         private.rate_limit;
begin
    -- Size and shape.
    if payload is null or length(payload) > 65536 then
        raise exception 'invalid payload' using errcode = '22023';
    end if;
    begin
        doc := payload::jsonb;
    exception when others then
        raise exception 'invalid payload' using errcode = '22023';
    end;
    if (doc ->> 'v') is distinct from '1' then
        raise exception 'unsupported version' using errcode = '22023';
    end if;
    plat := doc ->> 'platform';
    if plat is null or plat not in ('android', 'desktop', 'web') then
        raise exception 'invalid platform' using errcode = '22023';
    end if;
    -- Coarse device context: brand and ROM mapped onto fixed lists, versions range-checked.
    dev := doc -> 'device';
    if jsonb_typeof(dev) = 'object' then
        d_brand := lower(coalesce(dev ->> 'brand', 'unknown'));
        if d_brand <> 'unknown' and not (d_brand = any (private.known_brands())) then
            d_brand := 'other';
        end if;
        d_rom := lower(coalesce(dev ->> 'rom', 'unknown'));
        if d_rom <> 'unknown' and not (d_rom = any (private.known_roms())) then
            d_rom := 'other';
        end if;
        if jsonb_typeof(dev -> 'romVersion') = 'number'
           and (dev ->> 'romVersion') ~ '^\d{1,2}$' then
            d_rom_ver := (dev ->> 'romVersion')::smallint;
        end if;
        if jsonb_typeof(dev -> 'sdk') = 'number'
           and (dev ->> 'sdk') ~ '^\d{2}$' and (dev ->> 'sdk')::int between 21 and 60 then
            d_sdk := (dev ->> 'sdk')::smallint;
        end if;
    end if;

    events := doc -> 'events';
    if jsonb_typeof(events) is distinct from 'array' then
        raise exception 'invalid events' using errcode = '22023';
    end if;
    n_events := jsonb_array_length(events);
    if n_events < 1 or n_events > private.max_events() then
        raise exception 'invalid event count' using errcode = '22023';
    end if;

    -- Challenge signature and expiry.
    select value into key from private.secrets where name = 'pow_hmac';
    if challenge is null or sig is null
       or encode(extensions.hmac(convert_to(challenge, 'UTF8'), key, 'sha256'), 'hex') <> sig then
        raise exception 'invalid challenge' using errcode = '22023';
    end if;
    begin
        expiry := split_part(challenge, '.', 2)::bigint;
    exception when others then
        raise exception 'invalid challenge' using errcode = '22023';
    end;
    if expiry < extract(epoch from now())::bigint then
        raise exception 'challenge expired' using errcode = '22023';
    end if;

    -- Proof of work.
    h := extensions.digest(
        challenge || ':' || encode(extensions.digest(payload, 'sha256'), 'hex') || ':' || x::text,
        'sha256'
    );
    for i in 0..7 loop
        h_num := h_num * 256 + get_byte(h, i);
    end loop;
    target := floor(power(2::numeric, 64) / (private.pow_k() * n_events));
    if h_num >= target then
        raise exception 'insufficient proof of work' using errcode = '22023';
    end if;

    -- Replay protection.
    insert into private.used_nonces (nonce, expires_at)
    values (challenge, to_timestamp(expiry))
    on conflict do nothing;
    if not found then
        raise exception 'challenge already used' using errcode = '22023';
    end if;

    -- Rate limit by hmac(ip, daily salt). The raw IP is never stored.
    -- Client IP as seen by the proxy. Not x-forwarded-for: its first entry is whatever
    -- the client sent. IPv6 is grouped by /48: free tunnel brokers hand out whole /48s,
    -- and home ISPs give each customer a /56 or /48, so real users rarely share a group.
    ip := coalesce(
        nullif(trim((current_setting('request.headers', true)::json) ->> 'cf-connecting-ip'), ''),
        nullif(trim((current_setting('request.headers', true)::json) ->> 'sb-forwarded-for'), ''),
        'unknown'
    );
    begin
        if family(ip::inet) = 6 then
            ip_bucket := host(network(set_masklen(ip::inet, 48))) || '/48';
        else
            ip_bucket := host(ip::inet);
        end if;
    exception when others then
        ip_bucket := 'unknown';
    end;
    -- Fail loudly: without a client IP every request would share one rate-limit group.
    if ip_bucket = 'unknown' then
        raise exception 'no client ip' using errcode = '22023';
    end if;
    insert into private.daily_salt (day, salt)
    values (today, extensions.gen_random_bytes(32))
    on conflict do nothing;
    select s.salt into salt from private.daily_salt s where s.day = today;
    insert into private.weekly_salt (week, salt)
    values (this_week, extensions.gen_random_bytes(32))
    on conflict do nothing;
    select s.salt into week_salt from private.weekly_salt s where s.week = this_week;
    v_ip_hash := extensions.hmac(convert_to(ip_bucket, 'UTF8'), salt, 'sha256');

    insert into private.rate_limit as r (ip_hash, day, calls, pkgs)
    values (v_ip_hash, today, 1, n_events)
    on conflict (ip_hash, day) do update
        set calls = r.calls + 1, pkgs = r.pkgs + excluded.pkgs
    returning * into rl;
    if rl.calls > private.max_calls_per_day() or rl.pkgs > private.max_pkgs_per_day() then
        raise exception 'rate limited' using errcode = '22023';
    end if;

    -- Apply events. At most +1 per (event, package) per batch.
    min_week := this_week - (private.cohort_weeks() * 7);
    -- A reboot can't happen inside one request: survived_reboot and voided are dropped for
    -- packages uninstalled in the same batch.
    select coalesce(array_agg(e ->> 'p'), '{}') into batch_pkgs
    from jsonb_array_elements(events) as e
    where jsonb_typeof(e) = 'object' and e ->> 'e' = 'uninstalled';

    for ev in
        -- Uninstalls first, so a reinstall in the same batch finds its uninstall.
        select * from (
            select distinct on (e ->> 'e', e ->> 'p')
                e ->> 'e' as event,
                e ->> 'p' as pkg,
                e ->> 'w' as week,
                e ->> 'b' as bucket
            from jsonb_array_elements(events) as e
            where jsonb_typeof(e) = 'object'
        ) d
        order by d.event <> 'uninstalled'
    loop
        if ev.event is null or ev.event not in ('uninstalled', 'survived_reboot', 'reinstalled', 'voided')
           or ev.bucket is null or ev.bucket not in ('1', '2-10', '11+')
           or ev.pkg is null or length(ev.pkg) > 255
           or ev.pkg !~ '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$'
           or ev.week is null or ev.week !~ '^\d{4}-\d{2}-\d{2}$' then
            continue;
        end if;
        declare
            w   date;
        begin
            w := ev.week::date;
            if w <> date_trunc('week', w)::date or w < min_week or w > this_week then
                continue;
            end if;
            -- Clients send uninstalls within seconds, so their cohort week is the current
            -- (or, just after midnight on Monday, the previous) week. This makes the
            -- cohort week the week the server received the report, so the "spread over
            -- weeks" rules measure real time. survived / reinstalled must match an
            -- existing uninstall cell, so they inherit this limit.
            if ev.event = 'uninstalled' and w < this_week - 7 then
                continue;
            end if;
            if ev.event in ('survived_reboot', 'voided') and ev.pkg = any (batch_pkgs) then
                continue;
            end if;
            if exists (select 1 from private.pkg_flags f where f.pkg = ev.pkg and f.mode = 'ignore') then
                continue;
            end if;
            -- One uninstall report per (IP, package) per week; one report per (IP, event,
            -- package) per day for the other events. Survived, reinstalled and voided can
            -- never exceed the uninstalls of their cell, so limiting uninstalls bounds them.
            if ev.event = 'uninstalled' then
                insert into private.weekly_seen (h, week)
                values (substring(extensions.hmac(convert_to(ip_bucket || '|' || ev.event || '|' || ev.pkg, 'UTF8'),
                                                  week_salt, 'sha256') from 1 for 16), this_week)
                on conflict do nothing;
                if not found then
                    continue;
                end if;
            else
                insert into private.daily_seen (h, day)
                values (extensions.hmac(convert_to(ip_bucket || '|' || ev.event || '|' || ev.pkg, 'UTF8'),
                                        salt, 'sha256'), today)
                on conflict do nothing;
                if not found then
                    continue;
                end if;
            end if;

            if ev.event = 'uninstalled' then
                -- Accepted uninstalls are at most 1 week old, so their cell is never inside
                -- a merged block (span = 4) and the upsert only ever meets span = 1 rows.
                insert into private.pkg_counts as c
                    (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u)
                values (ev.pkg, w, ev.bucket, d_brand, d_rom, d_rom_ver, d_sdk, 1)
                on conflict (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk)
                do update set u = c.u + 1;
                accepted := accepted + 1;
                insert into private.pkg_daily_intake as i (day, pkg, event, n)
                values (today, ev.pkg, ev.event, 1)
                on conflict (day, pkg, event) do update set n = i.n + 1;
            else
                -- survived_reboot + voided, and reinstalled, may never exceed uninstalled in
                -- the same cell (a void takes an uninstall out of the survival denominator,
                -- so it counts against the same budget as a survival). The cell must exist,
                -- which means it has an uninstall; only weekly rows (span = 1) are updated.
                if ev.event = 'survived_reboot' then
                    update private.pkg_counts c
                    set s = c.s + 1
                    where c.pkg = ev.pkg and c.cohort_week = w and c.batch_bucket = ev.bucket
                      and c.brand = d_brand and c.rom = d_rom
                      and c.rom_version = d_rom_ver and c.sdk = d_sdk
                      and c.span = 1 and c.s + c.v < c.u;
                elsif ev.event = 'voided' then
                    update private.pkg_counts c
                    set v = c.v + 1
                    where c.pkg = ev.pkg and c.cohort_week = w and c.batch_bucket = ev.bucket
                      and c.brand = d_brand and c.rom = d_rom
                      and c.rom_version = d_rom_ver and c.sdk = d_sdk
                      and c.span = 1 and c.s + c.v < c.u;
                else
                    update private.pkg_counts c
                    set r = c.r + 1
                    where c.pkg = ev.pkg and c.cohort_week = w and c.batch_bucket = ev.bucket
                      and c.brand = d_brand and c.rom = d_rom
                      and c.rom_version = d_rom_ver and c.sdk = d_sdk
                      and c.span = 1 and c.r < c.u;
                end if;
                if found then
                    accepted := accepted + 1;
                    insert into private.pkg_daily_intake as i (day, pkg, event, n)
                    values (today, ev.pkg, ev.event, 1)
                    on conflict (day, pkg, event) do update set n = i.n + 1;
                end if;
            end if;
        exception when others then
            continue;
        end;
    end loop;

    insert into private.contributions as c (week, platform, n)
    values (this_week, plat, 1)
    on conflict (week, platform) do update set n = c.n + 1;

    return json_build_object('accepted', accepted);
end;
$$;

-- ---------------------------------------------------------------------------
-- Scores
-- ---------------------------------------------------------------------------

-- Voided uninstalls (the package came back without Canta: system update, restore) say
-- nothing about survival, so they leave the survival denominator: effective uninstalls
-- U' = U - V, survival s = S / U'. U' also drives the >= 30 / >= 200 thresholds, the Wilson
-- bounds of s, the baselines and the small-batch choice. Reinstalls stay R / U.
--
-- Only mature cohorts: older than cohort_weeks(), so every device had its full window
-- to report a reboot. Small-batch stats (buckets '1' and '2-10') are preferred once a
-- package has >= 30 small-batch uninstalls, because they attribute blame more clearly.
-- Rows are weekly cells (span = 1) or merged 4-week blocks (span = 4); the counters add up
-- the same way, only the "spread over weeks" rules need pkg_block_spread for merged weeks.
--
-- Scores are computed per scope: rom = 'all' (every device), per ROM family
-- (rom_version = -1, all versions pooled) and per (ROM family, major version). Each scope
-- has its own baseline, as reboot / reopen habits differ per ROM. Rows of the 'all' scope
-- also carry the package's main ROM family and its share of known-ROM uninstalls, and
-- family rows carry their main version and its share, so the app can tell when a pooled
-- verdict comes mostly from another ROM or another version.
-- Brand and API level are stored but not used for published verdicts yet.
--
-- Abuse resistance:
--   * Baselines only use packages with >= 30 uninstalls, so flooding random package
--     names can't move them.
--   * Any verdict needs uninstalls from >= 2 cohort weeks, none holding > 60% of them.
--   * "safe" (the dangerous direction) needs >= 200 uninstalls from >= 3 weeks, none
--     holding > 50%. A wrong "safe" can brick phones, a wrong warning can't.
--   * Packages flagged 'hide' are left out.
create materialized view private.package_scores as
with mature as (
    select c.* from private.pkg_counts c
    where c.cohort_week < date_trunc('week', now() at time zone 'utc')::date - (private.cohort_weeks() * 7)
      and not exists (select 1 from private.pkg_flags f where f.pkg = c.pkg and f.mode = 'hide')
),
scoped as (
    select pkg, 'all'::text as rom, 0::smallint as rom_version, cohort_week, span, batch_bucket, u, s, r, v from mature
    union all
    select pkg, rom, rom_version, cohort_week, span, batch_bucket, u, s, r, v from mature
    where rom not in ('unknown', 'other')
    union all
    select pkg, rom, -1::smallint, cohort_week, span, batch_bucket, u, s, r, v from mature
    where rom not in ('unknown', 'other')
),
rom_mix as (
    select distinct on (pkg)
        pkg,
        rom                                                  as main_rom,
        u_rom / sum(u_rom) over (partition by pkg)           as main_rom_share
    from (
        select pkg, rom, sum(u)::numeric as u_rom
        from mature
        where u > 0 and rom not in ('unknown', 'other')
        group by pkg, rom
    ) r
    order by pkg, u_rom desc, rom
),
version_mix as (
    select distinct on (pkg, rom)
        pkg, rom,
        rom_version                                           as main_version,
        u_ver / sum(u_ver) over (partition by pkg, rom)       as main_version_share
    from (
        select pkg, rom, rom_version, sum(u)::numeric as u_ver
        from mature
        where u > 0 and rom not in ('unknown', 'other')
        group by pkg, rom, rom_version
    ) v
    order by pkg, rom, u_ver desc, rom_version
),
-- Weekly uninstalls of the weeks that are still separate rows.
per_week as (
    select pkg, rom, rom_version, cohort_week, sum(u) as u_week
    from scoped where span = 1
    group by pkg, rom, rom_version, cohort_week
    having sum(u) > 0
),
-- All uninstalls of a scope, merged blocks included.
scope_total as (
    select pkg, rom, rom_version, sum(u) as total
    from scoped
    group by pkg, rom, rom_version
    having sum(u) > 0
),
spread as (
    select t.pkg, t.rom, t.rom_version,
           coalesce(w.weeks, 0) + coalesce(b.weeks, 0)                    as weeks,
           greatest(w.max_u, b.max_u)::numeric / t.total                  as max_week_share
    from scope_total t
    left join (
        select pkg, rom, rom_version, count(*) as weeks, max(u_week) as max_u
        from per_week
        group by pkg, rom, rom_version
    ) w using (pkg, rom, rom_version)
    left join (
        select pkg, rom, rom_version, sum(weeks) as weeks, max(max_week_u) as max_u
        from private.pkg_block_spread
        group by pkg, rom, rom_version
    ) b using (pkg, rom, rom_version)
),
per_pkg as (
    select
        pkg, rom, rom_version,
        sum(u)                                                          as u_all,
        sum(s)                                                          as s_all,
        sum(r)                                                          as r_all,
        sum(u) filter (where batch_bucket <> '11+')                     as u_small,
        sum(s) filter (where batch_bucket <> '11+')                     as s_small,
        sum(r) filter (where batch_bucket <> '11+')                     as r_small,
        coalesce(sum(v), 0)                                             as v_all,
        coalesce(sum(v) filter (where batch_bucket <> '11+'), 0)        as v_small
    from scoped
    group by pkg, rom, rom_version
),
-- Effective uninstalls U' = U - V.
eff as (
    select *,
        coalesce(u_all, 0) - v_all     as ue_all,
        coalesce(u_small, 0) - v_small as ue_small
    from per_pkg
),
baseline as (
    select
        rom, rom_version,
        coalesce(sum(s_all), 0) / nullif(sum(ue_all), 0)                                      as s_all,
        coalesce(sum(r_all), 0) / nullif(sum(u_all), 0)                                       as q_all,
        coalesce(sum(s_small) filter (where ue_small >= 30), 0)
            / nullif(sum(ue_small) filter (where ue_small >= 30), 0)                          as s_small,
        coalesce(sum(r_small) filter (where ue_small >= 30), 0)
            / nullif(sum(u_small) filter (where ue_small >= 30), 0)                           as q_small
    from eff
    where ue_all >= 30
    group by rom, rom_version
),
chosen as (
    select
        p.pkg, p.rom, p.rom_version,
        p.ue_small >= 30 as small_batch,
        case when p.ue_small >= 30 then p.ue_small else p.ue_all end::numeric as u,
        case when p.ue_small >= 30 then coalesce(p.u_small, 0) else coalesce(p.u_all, 0) end::numeric as u_raw,
        case when p.ue_small >= 30 then coalesce(p.s_small, 0) else coalesce(p.s_all, 0) end::numeric as s,
        case when p.ue_small >= 30 then coalesce(p.r_small, 0) else coalesce(p.r_all, 0) end::numeric as r,
        case when p.ue_small >= 30 then b.s_small else b.s_all end as s_base,
        case when p.ue_small >= 30 then b.q_small else b.q_all end as q_base,
        coalesce(sp.weeks, 0)            as weeks,
        coalesce(sp.max_week_share, 1)   as max_week_share
    from eff p
    left join baseline b using (rom, rom_version)
    left join spread sp using (pkg, rom, rom_version)
)
select
    c.pkg,
    c.rom,
    c.rom_version,
    u::bigint                         as uninstalls,
    round(s / nullif(u, 0), 4)        as survival,
    round(r / nullif(u_raw, 0), 4)    as reinstall,
    small_batch,
    case
        when u < 30 then 'insufficient'
        when s_base is null or q_base is null
             or weeks < 2 or max_week_share > 0.6 then 'unclear'
        when private.wilson_high(s, u) < 0.7 * s_base then 'bootloop'
        when private.wilson_low(r, u_raw) > 2 * q_base and r / u_raw >= 0.10 then 'breaks'
        when u >= 200 and weeks >= 3 and max_week_share <= 0.5
             and private.wilson_low(s, u) >= 0.9 * s_base
             and private.wilson_high(r, u_raw) < greatest(1.5 * q_base, 0.02) then 'safe'
        else 'unclear'
    end                               as verdict,
    now()                             as updated_at,
    case when c.rom = 'all' then m.main_rom end                       as main_rom,
    case when c.rom = 'all' then round(m.main_rom_share, 4) end       as main_rom_share,
    case when c.rom_version = -1 then vm.main_version end             as main_version,
    case when c.rom_version = -1 then round(vm.main_version_share, 4) end as main_version_share
from chosen c
left join rom_mix m on m.pkg = c.pkg
left join version_mix vm on vm.pkg = c.pkg and vm.rom = c.rom
where u >= 30;

create unique index on private.package_scores (pkg, rom, rom_version);

-- Scores across all devices, with the package's main ROM family and its share.
-- Deliberately coarse, to leak little about exact counts or how close a package is to a
-- verdict threshold: uninstalls are floored to hundreds (0 means fewer than 100), shares
-- have 2 decimals and the survival / reinstall rates are not returned at all. The verdicts
-- themselves are computed from the exact numbers in the materialized view.
create function public.package_scores()
returns table (
    pkg text, uninstalls bigint,
    small_batch boolean, verdict text, updated_at timestamptz,
    main_rom text, main_rom_share numeric
)
language sql
stable
security definer
set search_path = ''
as $$
    select pkg, (uninstalls / 100) * 100, small_batch, verdict, updated_at,
           main_rom, round(main_rom_share, 2)
    from private.package_scores
    where rom = 'all'
$$;

-- Scores for one ROM family and major version, e.g. ('hyperos', 1), or for the whole
-- family (all versions pooled) with version -1, e.g. ('hyperos', -1). Family rows also
-- return the version most reports came from and its share.
create function public.package_scores_for(p_rom text, p_rom_version int)
returns table (
    pkg text, uninstalls bigint,
    small_batch boolean, verdict text, updated_at timestamptz,
    main_version smallint, main_version_share numeric
)
language sql
stable
security definer
set search_path = ''
as $$
    select pkg, (uninstalls / 100) * 100, small_batch, verdict, updated_at,
           main_version, round(main_version_share, 2)
    from private.package_scores
    where rom = lower(p_rom) and rom <> 'all' and rom_version = p_rom_version
$$;

-- ---------------------------------------------------------------------------
-- Permissions (for the objects created above)
-- ---------------------------------------------------------------------------

revoke all on private.package_scores from public, anon, authenticated;
revoke all on function public.record(text, text, text, bigint) from public;
revoke all on function public.package_scores() from public;
revoke all on function public.package_scores_for(text, int) from public;
grant execute on function public.record(text, text, text, bigint) to anon, authenticated;
grant execute on function public.package_scores() to anon, authenticated;
grant execute on function public.package_scores_for(text, int) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Weekly compaction (UTC)
-- ---------------------------------------------------------------------------

-- 1. Merge: a 4-week block is merged once its last week is older than cohort_weeks() + 1
--    weeks, i.e. nothing can be added to it any more. The weekly rows are replaced by
--    their sums (span = 4, cohort_week = block start); first the numbers the spread rules
--    need from those weeks go to pkg_block_spread.
-- 2. Expire: a block that ended more than 52 weeks ago is deleted for every package that
--    has >= 200 effective uninstalls (u - v) in newer rows. Oldest block first, so each
--    check sees the result of the earlier deletions: just enough history stays to keep
--    200. The check is per package, not per ROM scope, so a ROM-specific verdict can lapse.
create function private.compact_counts() returns void
language plpgsql set search_path = '' as $$
declare
    this_week  date := date_trunc('week', now() at time zone 'utc')::date;
    limit_week date := date_trunc('week', now() at time zone 'utc')::date
                       - (private.cohort_weeks() + 1) * 7;
    blk        date;
begin
    for blk in
        select distinct private.block_of(cohort_week)
        from private.pkg_counts
        where span = 1 and private.block_of(cohort_week) + 21 < limit_week
        order by 1
    loop
        insert into private.pkg_block_spread as b (pkg, rom, rom_version, block, weeks, max_week_u)
        select pkg, rom, rom_version, blk, count(*)::smallint, max(u_week)::integer
        from (
            select pkg, rom, rom_version, cohort_week, sum(u) as u_week
            from (
                select pkg, 'all'::text as rom, 0::smallint as rom_version, cohort_week, u
                from private.pkg_counts
                where span = 1 and private.block_of(cohort_week) = blk
                union all
                select pkg, rom, rom_version, cohort_week, u
                from private.pkg_counts
                where span = 1 and private.block_of(cohort_week) = blk
                  and rom not in ('unknown', 'other')
                union all
                select pkg, rom, -1::smallint, cohort_week, u
                from private.pkg_counts
                where span = 1 and private.block_of(cohort_week) = blk
                  and rom not in ('unknown', 'other')
            ) x
            group by pkg, rom, rom_version, cohort_week
            having sum(u) > 0
        ) w
        group by pkg, rom, rom_version
        on conflict (pkg, rom, rom_version, block) do update
            set weeks = b.weeks + excluded.weeks,
                max_week_u = greatest(b.max_week_u, excluded.max_week_u);

        drop table if exists pg_temp.compact_block;
        create temp table compact_block on commit drop as
        select pkg, batch_bucket, brand, rom, rom_version, sdk,
               sum(u)::integer as u, sum(s)::integer as s,
               sum(r)::integer as r, sum(v)::integer as v
        from private.pkg_counts
        where private.block_of(cohort_week) = blk
        group by pkg, batch_bucket, brand, rom, rom_version, sdk;

        delete from private.pkg_counts where private.block_of(cohort_week) = blk;

        insert into private.pkg_counts
            (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, span, u, s, r, v)
        select pkg, blk, batch_bucket, brand, rom, rom_version, sdk, 4, u, s, r, v
        from pg_temp.compact_block;

        drop table pg_temp.compact_block;
    end loop;

    for blk in
        select distinct cohort_week
        from private.pkg_counts
        where span = 4 and cohort_week + 28 <= this_week - 52 * 7
        order by 1
    loop
        delete from private.pkg_counts c
        where c.span = 4 and c.cohort_week = blk
          and (select coalesce(sum(n.u - n.v), 0) from private.pkg_counts n
               where n.pkg = c.pkg and n.cohort_week > blk
                 and n.cohort_week < this_week - private.cohort_weeks() * 7) >= 200;

        delete from private.pkg_block_spread sp
        where sp.block = blk
          and (select coalesce(sum(n.u - n.v), 0) from private.pkg_counts n
               where n.pkg = sp.pkg and n.cohort_week > blk
                 and n.cohort_week < this_week - private.cohort_weeks() * 7) >= 200;
    end loop;
end;
$$;
revoke all on function private.compact_counts() from public, anon, authenticated;

-- Own job (like the daily purge): a failure here must not roll back anything else.
select cron.schedule(
    'canta-weekly-compact',
    '30 0 * * 1',
    $$ select private.compact_counts(); $$
);

-- Make PostgREST pick up the recreated functions.
notify pgrst, 'reload schema';
