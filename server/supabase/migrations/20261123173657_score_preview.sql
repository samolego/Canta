-- Provisional "preview" scores for the developer's debug builds.
--   * The scoring query moves into private.score_rows(p_before, p_spread), so the real and the
--     preview scores come from the same code and can't drift apart.
--   * private.package_scores is the same as before (same data gives the same rows).
--   * private.package_scores_preview uses every week except the current one and leaves the
--     "spread over weeks" rules off, so young data already gives verdicts.
--   * The preview is only readable with a secret key (private.secrets, 'preview_key').
-- Needs 20261114195746_cell_counters_and_blocks.sql applied first.

-- The scoring query, with two knobs: cohorts older than p_before are used, and p_spread
-- switches the "spread over weeks" rules (>= 2 weeks, none > 60%; safe: >= 3 weeks, none > 50%).
-- See 20261114195746_cell_counters_and_blocks.sql for the reasoning behind the rules.
create function private.score_rows(p_before date, p_spread boolean)
returns table (
    pkg text, rom text, rom_version smallint, uninstalls bigint,
    survival numeric, reinstall numeric, small_batch boolean, verdict text,
    updated_at timestamptz, main_rom text, main_rom_share numeric,
    main_version smallint, main_version_share numeric
)
language sql
stable
set search_path = ''
as $fn$
with mature as (
    select c.* from private.pkg_counts c
    where c.cohort_week < p_before
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
             or (p_spread and (weeks < 2 or max_week_share > 0.6)) then 'unclear'
        when private.wilson_high(s, u) < 0.7 * s_base then 'bootloop'
        when private.wilson_low(r, u_raw) > 2 * q_base and r / u_raw >= 0.10 then 'breaks'
        when u >= 200 and (not p_spread or (weeks >= 3 and max_week_share <= 0.5))
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
where u >= 30
$fn$;
revoke all on function private.score_rows(date, boolean) from public, anon, authenticated;

-- The public functions depend on the view: drop them first, recreate everything as it was.
drop function public.package_scores();
drop function public.package_scores_for(text, int);
drop materialized view private.package_scores;

create materialized view private.package_scores as
select * from private.score_rows(
    date_trunc('week', now() at time zone 'utc')::date - private.cohort_weeks() * 7, true);

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

revoke all on private.package_scores from public, anon, authenticated;
revoke all on function public.package_scores() from public;
revoke all on function public.package_scores_for(text, int) from public;
grant execute on function public.package_scores() to anon, authenticated;
grant execute on function public.package_scores_for(text, int) to anon, authenticated;

-- Preview: every week except the current one, spread rules off. Provisional, for the
-- developer's debug builds only: survival is undercounted in young weeks (devices have not had
-- their full window to report a reboot), but the baselines come from the same weeks, so the
-- comparison stays fair.
create materialized view private.package_scores_preview as
select * from private.score_rows(date_trunc('week', now() at time zone 'utc')::date, false);

create unique index on private.package_scores_preview (pkg, rom, rom_version);
revoke all on private.package_scores_preview from public, anon, authenticated;

-- Key for the preview functions. Read it with:
--   select encode(value, 'hex') from private.secrets where name = 'preview_key';
insert into private.secrets (name, value) values ('preview_key', extensions.gen_random_bytes(32))
on conflict do nothing;

-- Same coarse output as public.package_scores() / package_scores_for(), but from the preview.
-- The key is the only protection, so anon may call them; a wrong key raises 'forbidden'.
create function public.package_scores_preview(p_key text)
returns table (
    pkg text, uninstalls bigint,
    small_batch boolean, verdict text, updated_at timestamptz,
    main_rom text, main_rom_share numeric
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if p_key is null or p_key is distinct from
       (select encode(value, 'hex') from private.secrets where name = 'preview_key') then
        raise exception 'forbidden' using errcode = '42501';
    end if;
    return query
    select s.pkg, (s.uninstalls / 100) * 100, s.small_batch, s.verdict, s.updated_at,
           s.main_rom, round(s.main_rom_share, 2)
    from private.package_scores_preview s
    where s.rom = 'all';
end;
$$;

create function public.package_scores_preview_for(p_key text, p_rom text, p_rom_version int)
returns table (
    pkg text, uninstalls bigint,
    small_batch boolean, verdict text, updated_at timestamptz,
    main_version smallint, main_version_share numeric
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
    if p_key is null or p_key is distinct from
       (select encode(value, 'hex') from private.secrets where name = 'preview_key') then
        raise exception 'forbidden' using errcode = '42501';
    end if;
    return query
    select s.pkg, (s.uninstalls / 100) * 100, s.small_batch, s.verdict, s.updated_at,
           s.main_version, round(s.main_version_share, 2)
    from private.package_scores_preview s
    where s.rom = lower(p_rom) and s.rom <> 'all' and s.rom_version = p_rom_version;
end;
$$;

revoke all on function public.package_scores_preview(text) from public;
revoke all on function public.package_scores_preview_for(text, text, int) from public;
grant execute on function public.package_scores_preview(text) to anon, authenticated;
grant execute on function public.package_scores_preview_for(text, text, int) to anon, authenticated;

-- Same job as before (same name replaces it), plus the preview refresh.
select cron.schedule(
    'canta-daily-scores',
    '15 0 * * *',
    $$
    select private.check_spikes((now() at time zone 'utc')::date - 1);
    delete from private.pkg_daily_intake where day < (now() at time zone 'utc')::date - 29;
    refresh materialized view concurrently private.package_scores;
    refresh materialized view concurrently private.package_scores_preview;
    $$
);

notify pgrst, 'reload schema';
