-- The 'all' scope compares each package with the average of its own ROM mix instead of the
-- average over all devices (see mix_baseline in private.score_rows). Reports of a survived reboot
-- come back far less often on some ROMs (MIUI, Funtouch, ColorOS ~40-50% against One UI ~80%), so
-- packages removed mostly there looked like bootloops. Per-ROM scopes are unchanged.
-- Same signature and columns, so the views and public functions that use it stay as they are.

create or replace function private.score_rows(p_before date, p_spread boolean)
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
-- How often a "survived reboot" comes back differs a lot per ROM (One UI ~80%, MIUI and
-- Funtouch ~40%, probably background limits on the boot check). Compared with the average over
-- all devices, a package removed mostly on such ROMs looks like a bootloop. So the 'all' scope
-- compares each package with the average of the ROM families it was removed on, weighted by its
-- uninstalls there (survival by effective uninstalls, reinstalls by uninstalls). Unknown / other
-- ROMs, and families without a baseline, use the overall average. The per-ROM scopes already
-- have their own baselines and are unchanged.
mix as (
    select pkg,
        case when rom in ('unknown', 'other') then null else rom end                       as fam,
        sum(u) - sum(v)                                                                      as ue_all,
        sum(u)                                                                               as u_all,
        coalesce(sum(u - v) filter (where batch_bucket <> '11+'), 0)                         as ue_small,
        coalesce(sum(u) filter (where batch_bucket <> '11+'), 0)                             as u_small
    from mature
    group by 1, 2
),
mix_baseline as (
    select x.pkg,
        sum(x.ue_all * coalesce(f.s_all, g.s_all))
            / nullif(sum(x.ue_all) filter (where coalesce(f.s_all, g.s_all) is not null), 0)       as s_all,
        sum(x.u_all * coalesce(f.q_all, g.q_all))
            / nullif(sum(x.u_all) filter (where coalesce(f.q_all, g.q_all) is not null), 0)        as q_all,
        sum(x.ue_small * coalesce(f.s_small, g.s_small))
            / nullif(sum(x.ue_small) filter (where coalesce(f.s_small, g.s_small) is not null), 0) as s_small,
        sum(x.u_small * coalesce(f.q_small, g.q_small))
            / nullif(sum(x.u_small) filter (where coalesce(f.q_small, g.q_small) is not null), 0)  as q_small
    from mix x
    left join (select * from baseline where rom = 'all') g on true
    left join baseline f on f.rom = x.fam and f.rom_version = -1
    group by x.pkg
),
chosen as (
    select
        p.pkg, p.rom, p.rom_version,
        p.ue_small >= 30 as small_batch,
        case when p.ue_small >= 30 then p.ue_small else p.ue_all end::numeric as u,
        case when p.ue_small >= 30 then coalesce(p.u_small, 0) else coalesce(p.u_all, 0) end::numeric as u_raw,
        case when p.ue_small >= 30 then coalesce(p.s_small, 0) else coalesce(p.s_all, 0) end::numeric as s,
        case when p.ue_small >= 30 then coalesce(p.r_small, 0) else coalesce(p.r_all, 0) end::numeric as r,
        -- The 'all' scope compares with the average of the package's own ROM mix (see mix_baseline).
        case when p.ue_small >= 30 then coalesce(mb.s_small, b.s_small) else coalesce(mb.s_all, b.s_all) end as s_base,
        case when p.ue_small >= 30 then coalesce(mb.q_small, b.q_small) else coalesce(mb.q_all, b.q_all) end as q_base,
        coalesce(sp.weeks, 0)            as weeks,
        coalesce(sp.max_week_share, 1)   as max_week_share
    from eff p
    left join baseline b using (rom, rom_version)
    left join spread sp using (pkg, rom, rom_version)
    left join mix_baseline mb on mb.pkg = p.pkg and p.rom = 'all'
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

refresh materialized view private.package_scores;
refresh materialized view private.package_scores_preview;
