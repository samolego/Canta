-- Package limits, spike alert fix.
--   * max packages per IP per day: 3000 -> 1000
--   * uninstall reports: one per (IP, package) per week (other events stay per day)
--   * spike check is silent until there are 7 days of data, then compares a package with
--     its own average over the days that exist
--   * intake history kept 29 days instead of 60
-- Safe to run once on a database with the package scoring migrations applied.

create or replace function private.max_pkgs_per_day() returns int language sql immutable set search_path = '' as $$ select 1000 $$;

-- hmac(ip bucket | event | package, weekly salt), first 16 bytes: one uninstall report per
-- (IP, package) per week. Deleted weekly together with the salt.
create table private.weekly_salt (
    week date primary key,
    salt bytea not null
);
create table private.weekly_seen (
    h    bytea primary key,
    week date not null
);
alter table private.weekly_salt enable row level security;
alter table private.weekly_seen enable row level security;
revoke all on private.weekly_salt, private.weekly_seen from public, anon, authenticated;

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

revoke all on function public.record(text, text, text, bigint) from public;
grant execute on function public.record(text, text, text, bigint) to anon, authenticated;

-- Silent until there are 7 days of data. The average is over the days data exists (max 28).
-- A brand-new package with >= 20 reports a day still alerts.
-- Writes packages whose daily intake jumped far above their usual level to
-- private.alerts. Check them with: select * from private.alerts order by id desc;
-- Optional notification: store a webhook URL (e.g. an ntfy topic) with
--   insert into private.secrets (name, value)
--   values ('alert_webhook', convert_to('https://ntfy.sh/<your-topic>', 'UTF8'));
-- and new alerts are posted there as ntfy JSON (package names and counts only).
create or replace function private.check_spikes(p_day date) returns void
language plpgsql set search_path = '' as $$
declare
    url  text;
    msg  text;
    hist_days int;
begin
    select least(28, p_day - min(day)) into hist_days from private.pkg_daily_intake;
    if hist_days is null or hist_days < 7 then
        return;
    end if;

    with new_alerts as (
        insert into private.alerts (day, pkg, event, n, typical)
        select t.day, t.pkg, t.event, t.n, coalesce(h.typical, 0)
        from private.pkg_daily_intake t
        left join lateral (
            select coalesce(sum(p.n), 0) / hist_days::numeric as typical
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

-- Alerts so far all come from the first days, when every package had no history.
delete from private.alerts;

-- Same jobs as before (same name replaces the job), with the weekly deletes and 29 days of
-- intake. The spike check reads 28 days back from yesterday.
select cron.schedule(
    'canta-daily-purge',
    '5 0 * * *',
    $$
    delete from private.daily_salt where day < (now() at time zone 'utc')::date;
    delete from private.rate_limit where day < (now() at time zone 'utc')::date;
    delete from private.daily_seen where day < (now() at time zone 'utc')::date;
    delete from private.weekly_seen where week < date_trunc('week', now() at time zone 'utc')::date;
    delete from private.weekly_salt where week < date_trunc('week', now() at time zone 'utc')::date;
    delete from private.used_nonces where expires_at < now();
    $$
);

select cron.schedule(
    'canta-daily-scores',
    '15 0 * * *',
    $$
    select private.check_spikes((now() at time zone 'utc')::date - 1);
    delete from private.pkg_daily_intake where day < (now() at time zone 'utc')::date - 29;
    refresh materialized view concurrently private.package_scores;
    $$
);
