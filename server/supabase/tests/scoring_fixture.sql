-- Shows how the scoring behaves on made-up counts, without touching real data.
-- Paste into the Supabase SQL editor (or psql) and run: everything is rolled back.
--
-- Uninstall counts below are the raw U; the scores use U' = U - voided (floored to hundreds in the output).
--
-- Counts are spread over 4 mature cohort weeks (per week values below), except
-- com.fx.burst, which arrives in a single week.
--
-- Expected (overall scope 'all'):
--   com.fx.base*   unclear     average packages: not clearly better or worse than the baseline
--   com.fx.boot    bootloop    few phones report back after a reboot
--   com.fx.breaks  breaks      reinstalled often
--   com.fx.safe    safe        survives as often as the average, never reinstalled, >= 200 reports
--   com.fx.big     unclear     only seen in big batches (small_batch = false)
--   com.fx.burst   unclear     everything came in one week, so no verdict
--   com.fx.void    unclear     most uninstalls were voided (an OS update restored the package), so
--                              U' = 80 and s = 60 / 80; without voids it would be 60 / 200, a false bootloop
--   com.fx.voidloop bootloop    voids don't hide a real bootloop: U' = 120, s = 8 / 120
--   com.fx.few     (hidden)    fewer than 30 reports
-- Per ROM: com.fx.rom is 'bootloop' on HyperOS 1 and 'safe' on HyperOS 2.

begin;

do $$
declare
    first_week date := date_trunc('week', now() at time zone 'utc')::date - 7 * 11;
begin
    -- 20 average packages on two ROM versions, to give every scope a baseline.
    insert into private.pkg_counts (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk, n)
    select 'com.fx.base' || g, e, first_week + 7 * wk, '1', 'xiaomi', 'hyperos', v, 34, n
    from generate_series(1, 20) g,
         generate_series(0, 3) wk,
         (values ('uninstalled', 50), ('survived_reboot', 25), ('reinstalled', 1)) x(e, n),
         (values (1), (2)) r(v);

    insert into private.pkg_counts (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk, n)
    select x.pkg, x.e, first_week + 7 * wk, x.b, 'xiaomi', 'hyperos', x.v, 34, x.n
    from generate_series(0, 3) wk,
         (values
            ('com.fx.boot',   'uninstalled',     '1',   1, 25),
            ('com.fx.boot',   'survived_reboot', '1',   1, 2),
            ('com.fx.breaks', 'uninstalled',     '1',   1, 25),
            ('com.fx.breaks', 'survived_reboot', '1',   1, 12),
            ('com.fx.breaks', 'reinstalled',     '1',   1, 8),
            ('com.fx.safe',   'uninstalled',     '1',   1, 75),
            ('com.fx.safe',   'survived_reboot', '1',   1, 40),
            ('com.fx.big',    'uninstalled',     '11+', 1, 25),
            ('com.fx.big',    'survived_reboot', '11+', 1, 12),
            ('com.fx.void',     'uninstalled',     '1', 1, 50),
            ('com.fx.void',     'survived_reboot', '1', 1, 15),
            ('com.fx.void',     'voided',          '1', 1, 30),
            ('com.fx.voidloop', 'uninstalled',     '1', 1, 50),
            ('com.fx.voidloop', 'survived_reboot', '1', 1, 2),
            ('com.fx.voidloop', 'voided',          '1', 1, 20),
            ('com.fx.few',    'uninstalled',     '1',   1, 2),
            ('com.fx.rom',    'uninstalled',     '1',   1, 25),
            ('com.fx.rom',    'survived_reboot', '1',   1, 2),
            ('com.fx.rom',    'uninstalled',     '1',   2, 100),
            ('com.fx.rom',    'survived_reboot', '1',   2, 53)
         ) x(pkg, e, b, v, n);

    insert into private.pkg_counts (pkg, event, cohort_week, batch_bucket, brand, rom, rom_version, sdk, n) values
        ('com.fx.burst', 'uninstalled',     first_week, '1', 'xiaomi', 'hyperos', 1, 34, 300),
        ('com.fx.burst', 'survived_reboot', first_week, '1', 'xiaomi', 'hyperos', 1, 34, 160);
end;
$$;

refresh materialized view private.package_scores;

select 'all' as scope, pkg, uninstalls, small_batch, verdict
from public.package_scores()
where pkg not like 'com.fx.base%' or pkg = 'com.fx.base1'
union all
select 'hyperos 1', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 1) where pkg = 'com.fx.rom'
union all
select 'hyperos 2', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 2) where pkg = 'com.fx.rom'
order by scope, pkg;

rollback;
