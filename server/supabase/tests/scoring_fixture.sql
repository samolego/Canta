-- Shows how the scoring behaves on made-up counts, without touching real data.
-- Paste into the Supabase SQL editor (or psql) and run: everything is rolled back.
--
-- Each row is one cell with its counters u / s / r / v. Uninstall counts below are the raw U; the
-- scores use U' = U - voided (floored to hundreds in the output).
--
-- Counts are spread over 4 mature cohort weeks (per week values below), except
-- com.fx.burst, which arrives in a single week. com.fx.oldbusy / com.fx.oldrare also have
-- 4 weeks of data that is 60-63 weeks old (see the expiry cases below).
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
--
-- Compaction: private.compact_counts() runs after the first result set. The fixture weeks are
-- 8-11 weeks old, so they can be merged into 4-week blocks; which weeks end up in the same block
-- depends on the date the script runs (the block grid is fixed), and a block is only merged once
-- its last week is older than 6 weeks, so the newest fixture week may stay a weekly row. The
-- block check query below shows what was merged. The second result set (phase 'after compact')
-- must be identical to the first (phase 'before compact'), except that com.fx.oldbusy is left
-- out of both: it loses its old rows on purpose, so its numbers change.
--
-- Expiry (blocks that ended more than 52 weeks ago are deleted when the package has >= 200
-- effective uninstalls in newer data), shown by the last two queries:
--   com.fx.oldbusy  old rows are gone after compaction (oldest week < 52 weeks old): 360 effective
--                   uninstalls in the newer weeks
--   com.fx.oldrare  old rows are kept (oldest week > 52 weeks old) and the verdict stays 'safe':
--                   only 80 newer uninstalls, and the old weeks still count for spread (8 weeks)
-- The oldest week after compaction is a block start, so it can be a few weeks older than the
-- weekly value before.
-- Note: compact_counts() works on the whole table, so on a real database it also merges real
-- rows, inside this transaction (rolled back at the end).

begin;

do $$
declare
    this_week  date := date_trunc('week', now() at time zone 'utc')::date;
    first_week date := date_trunc('week', now() at time zone 'utc')::date - 7 * 11;
    old_week   date := date_trunc('week', now() at time zone 'utc')::date - 7 * 63;
begin
    -- 20 average packages on two ROM versions, to give every scope a baseline.
    insert into private.pkg_counts (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u, s, r)
    select 'com.fx.base' || g, first_week + 7 * wk, '1', 'xiaomi', 'hyperos', v, 34, 50, 25, 1
    from generate_series(1, 20) g,
         generate_series(0, 3) wk,
         (values (1), (2)) r(v);

    insert into private.pkg_counts (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u, s, r, v)
    select x.pkg, first_week + 7 * wk, x.b, 'xiaomi', 'hyperos', x.ver, 34, x.u, x.s, x.r, x.v
    from generate_series(0, 3) wk,
         (values
            ('com.fx.boot',     '1',   1, 25,  2, 0,  0),
            ('com.fx.breaks',   '1',   1, 25, 12, 8,  0),
            ('com.fx.safe',     '1',   1, 75, 40, 0,  0),
            ('com.fx.big',      '11+', 1, 25, 12, 0,  0),
            ('com.fx.void',     '1',   1, 50, 15, 0, 30),
            ('com.fx.voidloop', '1',   1, 50,  2, 0, 20),
            ('com.fx.few',      '1',   1,  2,  0, 0,  0),
            ('com.fx.rom',      '1',   1, 25,  2, 0,  0),
            ('com.fx.rom',      '1',   2, 100, 53, 0, 0)
         ) x(pkg, b, ver, u, s, r, v);

    insert into private.pkg_counts (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u, s) values
        ('com.fx.burst', first_week, '1', 'xiaomi', 'hyperos', 1, 34, 300, 160);

    -- Expiry cases, on their own ROM so the HyperOS baselines stay as above. Old data
    -- (60-63 weeks old) plus newer mature weeks (8-11 weeks old).
    insert into private.pkg_counts (pkg, cohort_week, batch_bucket, brand, rom, rom_version, sdk, u, s, r, v)
    select x.pkg, x.fw + 7 * wk, '1', 'oppo', 'coloros', 1, 34, x.u, x.s, 0, x.v
    from generate_series(0, 3) wk,
         (values
            ('com.fx.oldbusy', old_week,   50, 25,  0),
            ('com.fx.oldbusy', first_week, 100, 50, 10),
            ('com.fx.oldrare', old_week,   50, 30,  0),
            ('com.fx.oldrare', first_week, 20, 12,  0)
         ) x(pkg, fw, u, s, v);
end;
$$;

refresh materialized view private.package_scores;

-- Merge check: fixture weeks, their block, and whether compact_counts() will merge them.
select cohort_week, private.block_of(cohort_week) as block,
       private.block_of(cohort_week) + 21
           < date_trunc('week', now() at time zone 'utc')::date - (private.cohort_weeks() + 1) * 7 as will_merge,
       count(*) as cells
from private.pkg_counts
where pkg like 'com.fx.%'
group by cohort_week
order by cohort_week;

-- Oldest week per expiry package, before compaction.
select 'before compact' as phase, pkg, min(cohort_week) as oldest_week,
       (date_trunc('week', now() at time zone 'utc')::date - min(cohort_week)) / 7 as oldest_age_weeks,
       count(*) as rows
from private.pkg_counts
where pkg in ('com.fx.oldbusy', 'com.fx.oldrare')
group by pkg
order by pkg;

select 'before compact' as phase, 'all' as scope, pkg, uninstalls, small_batch, verdict
from public.package_scores()
where (pkg not like 'com.fx.base%' or pkg = 'com.fx.base1') and pkg <> 'com.fx.oldbusy'
union all
select 'before compact', 'hyperos 1', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 1) where pkg = 'com.fx.rom'
union all
select 'before compact', 'hyperos 2', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 2) where pkg = 'com.fx.rom'
order by scope, pkg;

select private.compact_counts();
refresh materialized view private.package_scores;

-- After compaction: block rows (span = 4) per fixture package, and the spread rows.
select pkg, count(*) filter (where span = 4) as block_rows, count(*) filter (where span = 1) as weekly_rows
from private.pkg_counts
where pkg like 'com.fx.%'
group by pkg
order by pkg;

select 'after compact' as phase, 'all' as scope, pkg, uninstalls, small_batch, verdict
from public.package_scores()
where (pkg not like 'com.fx.base%' or pkg = 'com.fx.base1') and pkg <> 'com.fx.oldbusy'
union all
select 'after compact', 'hyperos 1', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 1) where pkg = 'com.fx.rom'
union all
select 'after compact', 'hyperos 2', pkg, uninstalls, small_batch, verdict
from public.package_scores_for('hyperos', 2) where pkg = 'com.fx.rom'
order by scope, pkg;

-- Expiry result: oldbusy younger than 52 weeks, oldrare older than 52 weeks. Also its score.
select 'after compact' as phase, c.pkg, min(c.cohort_week) as oldest_week,
       (date_trunc('week', now() at time zone 'utc')::date - min(c.cohort_week)) / 7 as oldest_age_weeks,
       count(*) as rows,
       (select s.verdict from private.package_scores s
        where s.pkg = c.pkg and s.rom = 'all') as verdict
from private.pkg_counts c
where c.pkg in ('com.fx.oldbusy', 'com.fx.oldrare')
group by c.pkg
order by c.pkg;

-- Preview scores (every week but the current one, spread rules off), scope 'all'.
refresh materialized view private.package_scores_preview;

select 'preview' as phase, 'all' as scope, pkg, uninstalls, small_batch, verdict
from private.package_scores_preview
where rom = 'all' and pkg like 'com.fx.%' and (pkg not like 'com.fx.base%' or pkg = 'com.fx.base1')
order by pkg;

rollback;
