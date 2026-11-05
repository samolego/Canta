-- Canta anonymous package safety scoring.
--
-- Privacy model:
--   * Only aggregate counters are stored: (package, event, cohort week, batch bucket,
--     brand, ROM family, ROM major version, Android API level) -> n.
--     Brands outside a fixed list of major manufacturers are stored as 'other'.
--     There are no device or install ids.
--   * Client IPs are never stored. Rate limiting uses hmac(ip, daily salt), with IPv6
--     grouped by /48. The same keyed hash, combined with the package, allows one report
--     per (IP, event, package) per day. Salts and these daily rows are deleted daily.
--     Caveat: the salt lives in this database, so for up to 24 hours anyone with database
--     access could test whether a known IP reported a known package. Once the day's salt
--     is deleted, that is no longer possible.
--   * Everything lives in the `private` schema (not exposed by the API). Clients can only
--     call public.challenge(), public.record(), public.package_scores() and
--     public.package_scores_for().
--
-- Anti-abuse: payload validation and caps, hashed-IP rate limit and per-package daily
-- dedup, a proof of work (a speed bump, not a wall), verdicts that need reports spread
-- over several weeks (much more for "safe"), and an admin flag table to freeze or hide
-- packages. Spikes in daily intake are written to private.alerts (and optionally posted
-- to a webhook, see check_spikes).

create extension if not exists pgcrypto with schema extensions;
create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net with schema extensions;

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table private.pkg_counts (
    pkg          text not null,
    event        text not null check (event in ('uninstalled', 'survived_reboot', 'reinstalled', 'voided')),
    cohort_week  date not null,
    batch_bucket text not null check (batch_bucket in ('1', '2-10', '11+')),
    brand        text not null,
    rom          text not null,
    rom_version  smallint not null,
    sdk          smallint not null,
    n            bigint not null default 0,
    primary key (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk)
);

create table private.contributions (
    week     date not null,
    platform text not null check (platform in ('android', 'desktop', 'web')),
    n        bigint not null default 0,
    primary key (week, platform)
);

-- Server-only secrets (proof-of-work HMAC key). Never sent to clients.
create table private.secrets (
    name  text primary key,
    value bytea not null
);
insert into private.secrets (name, value) values ('pow_hmac', extensions.gen_random_bytes(32));

-- One salt per UTC day, used to hash client IPs. Old salts are deleted daily.
create table private.daily_salt (
    day  date primary key,
    salt bytea not null
);

create table private.rate_limit (
    ip_hash bytea not null,
    day     date not null,
    calls   int not null default 0,
    pkgs    int not null default 0,
    primary key (ip_hash, day)
);

create table private.used_nonces (
    nonce      text primary key,
    expires_at timestamptz not null
);

-- hmac(ip bucket | event | package, daily salt): one report per (IP, event, package) per
-- day. Uninstall, reinstall, survived and voided reports of the same package from one IP still
-- all count. Deleted daily together with the salt.
create table private.daily_seen (
    h   bytea primary key,
    day date not null
);

-- Admin flags, managed by hand in the SQL editor:
--   'ignore': new reports for the package are dropped (freeze)
--   'hide':   the package is left out of the published scores
create table private.pkg_flags (
    pkg        text primary key,
    mode       text not null check (mode in ('ignore', 'hide')),
    reason     text not null default '',
    created_at timestamptz not null default now()
);

-- Accepted reports per package and day, for spike alerts. Kept 60 days.
create table private.pkg_daily_intake (
    day   date not null,
    pkg   text not null,
    event text not null,
    n     bigint not null default 0,
    primary key (day, pkg, event)
);

create table private.alerts (
    id         bigint generated always as identity primary key,
    created_at timestamptz not null default now(),
    day        date not null,
    pkg        text not null,
    event      text not null,
    n          bigint not null,
    typical    numeric not null
);

alter table private.pkg_counts enable row level security;
alter table private.contributions enable row level security;
alter table private.secrets enable row level security;
alter table private.daily_salt enable row level security;
alter table private.rate_limit enable row level security;
alter table private.used_nonces enable row level security;
alter table private.daily_seen enable row level security;
alter table private.pkg_flags enable row level security;
alter table private.pkg_daily_intake enable row level security;
alter table private.alerts enable row level security;

-- ---------------------------------------------------------------------------
-- Constants
-- ---------------------------------------------------------------------------

-- Expected hashes per event. 100 events ~ 1M sha256 hashes ~ 1 s on a mid-range phone.
create function private.pow_k() returns numeric language sql immutable set search_path = '' as $$ select 10000::numeric $$;
create function private.max_events() returns int language sql immutable set search_path = '' as $$ select 250 $$;
create function private.max_calls_per_day() returns int language sql immutable set search_path = '' as $$ select 100 $$;
create function private.max_pkgs_per_day() returns int language sql immutable set search_path = '' as $$ select 3000 $$;
-- Coarse device context: anything outside these lists is stored as 'other'.
create function private.known_brands() returns text[] language sql immutable set search_path = '' as $$
    select array['xiaomi', 'samsung', 'google', 'oneplus', 'oppo', 'realme', 'vivo', 'motorola',
                 'huawei', 'honor', 'sony', 'nokia', 'asus', 'nothing', 'fairphone', 'zte',
                 'lenovo', 'tecno', 'infinix']
$$;
create function private.known_roms() returns text[] language sql immutable set search_path = '' as $$
    select array['hyperos', 'miui', 'oneui', 'coloros', 'realmeui', 'originos', 'funtouch',
                 'magicos', 'emui', 'oxygenos', 'nothingos', 'stock']
$$;
create function private.cohort_weeks() returns int language sql immutable set search_path = '' as $$ select 5 $$;

-- ---------------------------------------------------------------------------
-- Proof-of-work challenge
-- ---------------------------------------------------------------------------

-- Returns {"challenge": "<hex nonce>.<expiry epoch>", "sig": "<hex hmac>"}.
-- Stateless: validity is proven by the HMAC, replay is prevented in record().
create function public.challenge()
returns json
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
    c text;
    key bytea;
begin
    select value into key from private.secrets where name = 'pow_hmac';
    c := encode(extensions.gen_random_bytes(16), 'hex') || '.' ||
         (extract(epoch from now())::bigint + 600)::text;
    return json_build_object(
        'challenge', c,
        'sig', encode(extensions.hmac(convert_to(c, 'UTF8'), key, 'sha256'), 'hex')
    );
end;
$$;

-- ---------------------------------------------------------------------------
-- Recording events
-- ---------------------------------------------------------------------------

-- payload: exact JSON text the client hashed, e.g.
--   {"v":1,"platform":"android","device":{"brand":"xiaomi","rom":"hyperos","romVersion":1,"sdk":34},
--    "events":[{"e":"uninstalled","p":"com.foo","w":"2026-10-26","b":"11+"}]}
-- PoW: first 8 bytes (big-endian, unsigned) of
--   sha256(challenge || ':' || hex(sha256(payload)) || ':' || x)
-- must be below floor(2^64 / (pow_k * number_of_events)).
create function public.record(payload text, challenge text, sig text, x bigint)
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
            grp text[];
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
            -- One report per (IP, event, package) per day.
            insert into private.daily_seen (h, day)
            values (extensions.hmac(convert_to(ip_bucket || '|' || ev.event || '|' || ev.pkg, 'UTF8'),
                                    salt, 'sha256'), today)
            on conflict do nothing;
            if not found then
                continue;
            end if;

            if ev.event = 'uninstalled' then
                insert into private.pkg_counts as c
                    (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk, n)
                values (ev.pkg, ev.event, w, ev.bucket, d_brand, d_rom, d_rom_ver, d_sdk, 1)
                on conflict (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk)
                do update set n = c.n + 1;
                accepted := accepted + 1;
                insert into private.pkg_daily_intake as i (day, pkg, event, n)
                values (today, ev.pkg, ev.event, 1)
                on conflict (day, pkg, event) do update set n = i.n + 1;
            else
                -- survived_reboot + voided, and reinstalled, may never exceed uninstalled in
                -- the same cell (a void takes an uninstall out of the survival denominator,
                -- so it counts against the same budget as a survival).
                grp := case when ev.event in ('survived_reboot', 'voided')
                            then array['survived_reboot', 'voided'] else array[ev.event] end;
                update private.pkg_counts c
                set n = c.n + 1
                where c.pkg = ev.pkg and c.event = ev.event
                  and c.cohort_week = w and c.batch_bucket = ev.bucket
                  and c.brand = d_brand and c.rom = d_rom
                  and c.rom_version = d_rom_ver and c.sdk = d_sdk
                  and (select coalesce(sum(g.n), 0) from private.pkg_counts g
                       where g.pkg = ev.pkg and g.event = any (grp)
                         and g.cohort_week = w and g.batch_bucket = ev.bucket
                         and g.brand = d_brand and g.rom = d_rom
                         and g.rom_version = d_rom_ver and g.sdk = d_sdk)
                      < (select u.n from private.pkg_counts u
                         where u.pkg = ev.pkg and u.event = 'uninstalled'
                           and u.cohort_week = w and u.batch_bucket = ev.bucket
                           and u.brand = d_brand and u.rom = d_rom
                           and u.rom_version = d_rom_ver and u.sdk = d_sdk);
                if not found then
                    insert into private.pkg_counts
                        (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk, n)
                    select ev.pkg, ev.event, w, ev.bucket, d_brand, d_rom, d_rom_ver, d_sdk, 1
                    where exists (select 1 from private.pkg_counts u
                                  where u.pkg = ev.pkg and u.event = 'uninstalled'
                                    and u.cohort_week = w and u.batch_bucket = ev.bucket
                                    and u.brand = d_brand and u.rom = d_rom
                                    and u.rom_version = d_rom_ver and u.sdk = d_sdk
                                    and u.n > (select coalesce(sum(g.n), 0) from private.pkg_counts g
                                               where g.pkg = ev.pkg and g.event = any (grp)
                                                 and g.cohort_week = w and g.batch_bucket = ev.bucket
                                                 and g.brand = d_brand and g.rom = d_rom
                                                 and g.rom_version = d_rom_ver and g.sdk = d_sdk))
                    on conflict do nothing;
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

create function private.wilson_low(x numeric, n numeric) returns numeric
language sql immutable set search_path = '' as $$
    select case when n = 0 then 0 else
        ((x / n) + 1.9208 / n - 1.96 * sqrt((x / n) * (1 - x / n) / n + 0.9604 / (n * n)))
        / (1 + 3.8416 / n) end
$$;

create function private.wilson_high(x numeric, n numeric) returns numeric
language sql immutable set search_path = '' as $$
    select case when n = 0 then 1 else
        ((x / n) + 1.9208 / n + 1.96 * sqrt((x / n) * (1 - x / n) / n + 0.9604 / (n * n)))
        / (1 + 3.8416 / n) end
$$;

-- Voided uninstalls (the package came back without Canta: system update, restore) say
-- nothing about survival, so they leave the survival denominator: effective uninstalls
-- U' = U - V, survival s = S / U'. U' also drives the >= 30 / >= 200 thresholds, the Wilson
-- bounds of s, the baselines and the small-batch choice. Reinstalls stay R / U.
--
-- Only mature cohorts: older than cohort_weeks(), so every device had its full window
-- to report a reboot. Small-batch stats (buckets '1' and '2-10') are preferred once a
-- package has >= 30 small-batch uninstalls, because they attribute blame more clearly.
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
    select pkg, 'all'::text as rom, 0::smallint as rom_version, cohort_week, event, batch_bucket, n from mature
    union all
    select pkg, rom, rom_version, cohort_week, event, batch_bucket, n from mature
    where rom not in ('unknown', 'other')
    union all
    select pkg, rom, -1::smallint, cohort_week, event, batch_bucket, n from mature
    where rom not in ('unknown', 'other')
),
rom_mix as (
    select distinct on (pkg)
        pkg,
        rom                                                  as main_rom,
        u_rom / sum(u_rom) over (partition by pkg)           as main_rom_share
    from (
        select pkg, rom, sum(n)::numeric as u_rom
        from mature
        where event = 'uninstalled' and rom not in ('unknown', 'other')
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
        select pkg, rom, rom_version, sum(n)::numeric as u_ver
        from mature
        where event = 'uninstalled' and rom not in ('unknown', 'other')
        group by pkg, rom, rom_version
    ) v
    order by pkg, rom, u_ver desc, rom_version
),
per_week as (
    select pkg, rom, rom_version, cohort_week, sum(n) as u_week
    from scoped where event = 'uninstalled'
    group by pkg, rom, rom_version, cohort_week
),
spread as (
    select pkg, rom, rom_version,
           count(*)                            as weeks,
           max(u_week)::numeric / sum(u_week)  as max_week_share
    from per_week
    group by pkg, rom, rom_version
),
per_pkg as (
    select
        pkg, rom, rom_version,
        sum(n) filter (where event = 'uninstalled')                                  as u_all,
        sum(n) filter (where event = 'survived_reboot')                              as s_all,
        sum(n) filter (where event = 'reinstalled')                                  as r_all,
        sum(n) filter (where event = 'uninstalled' and batch_bucket <> '11+')        as u_small,
        sum(n) filter (where event = 'survived_reboot' and batch_bucket <> '11+')    as s_small,
        sum(n) filter (where event = 'reinstalled' and batch_bucket <> '11+')        as r_small,
        coalesce(sum(n) filter (where event = 'voided'), 0)                          as v_all,
        coalesce(sum(n) filter (where event = 'voided' and batch_bucket <> '11+'), 0) as v_small
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
-- Permissions
-- ---------------------------------------------------------------------------

revoke all on all tables in schema private from public, anon, authenticated;
revoke all on all functions in schema private from public, anon, authenticated;
revoke all on all sequences in schema private from public, anon, authenticated;

revoke all on function public.challenge() from public;
revoke all on function public.record(text, text, text, bigint) from public;
revoke all on function public.package_scores() from public;
revoke all on function public.package_scores_for(text, int) from public;
grant execute on function public.challenge() to anon, authenticated;
grant execute on function public.record(text, text, text, bigint) to anon, authenticated;
grant execute on function public.package_scores() to anon, authenticated;
grant execute on function public.package_scores_for(text, int) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Daily maintenance (UTC)
-- ---------------------------------------------------------------------------

-- Writes packages whose daily intake jumped far above their usual level to
-- private.alerts. Check them with: select * from private.alerts order by id desc;
-- Optional notification: store a webhook URL (e.g. an ntfy topic) with
--   insert into private.secrets (name, value)
--   values ('alert_webhook', convert_to('https://ntfy.sh/<your-topic>', 'UTF8'));
-- and new alerts are posted there as ntfy JSON (package names and counts only).
create function private.check_spikes(p_day date) returns void
language plpgsql set search_path = '' as $$
declare
    url  text;
    msg  text;
begin
    with new_alerts as (
        insert into private.alerts (day, pkg, event, n, typical)
        select t.day, t.pkg, t.event, t.n, coalesce(h.typical, 0)
        from private.pkg_daily_intake t
        left join lateral (
            select coalesce(sum(p.n), 0) / 28.0 as typical
            from private.pkg_daily_intake p
            where p.pkg = t.pkg and p.event = t.event
              and p.day between p_day - 28 and p_day - 1
        ) h on true
        where t.day = p_day
          and t.n >= 20
          and t.n > 5 * coalesce(h.typical, 0) + 10
        returning pkg, event, n, typical
    )
    select string_agg(format('%s %s: %s (usual %s/day)', pkg, event, n, round(typical, 1)), E'\n')
    into msg from new_alerts;

    select convert_from(value, 'UTF8') into url from private.secrets where name = 'alert_webhook';
    if msg is not null and url is not null then
        -- ntfy JSON publishing: POST {topic, title, message} to the server root.
        perform net.http_post(
            url := regexp_replace(url, '/[^/]+/?$', ''),
            body := jsonb_build_object(
                'topic', substring(url from '/([^/]+)/?$'),
                'title', 'Canta spike alert for ' || p_day,
                'message', msg
            )
        );
    end if;
end;
$$;
revoke all on function private.check_spikes(date) from public, anon, authenticated;

-- Privacy deletes run as their own job: a cron command is one transaction, so a failure
-- in the spike check (pg_net) or the view refresh must not roll these back.
select cron.schedule(
    'canta-daily-purge',
    '5 0 * * *',
    $$
    delete from private.daily_salt where day < (now() at time zone 'utc')::date;
    delete from private.rate_limit where day < (now() at time zone 'utc')::date;
    delete from private.daily_seen where day < (now() at time zone 'utc')::date;
    delete from private.used_nonces where expires_at < now();
    $$
);

select cron.schedule(
    'canta-daily-scores',
    '15 0 * * *',
    $$
    select private.check_spikes((now() at time zone 'utc')::date - 1);
    delete from private.pkg_daily_intake where day < (now() at time zone 'utc')::date - 60;
    refresh materialized view concurrently private.package_scores;
    $$
);
