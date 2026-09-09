import type { DailyAnalyticsSummary } from '../api/analytics.types'
import { analyzeProductivity, describeProductivityPattern, secondsToHours, sortDailySummary, totalActivity } from './productivityAnalysis'

const day = (date: string, productivity: number | null, codingDurationSeconds: number, overrides: Partial<DailyAnalyticsSummary> = {}): DailyAnalyticsSummary => ({
  date, productivity, codingDurationSeconds, workoutCount: 0, workoutDurationSeconds: 0, caloriesBurned: 0,
  githubActivityCount: 0, codingSessionCount: 0, mood: null, energy: null, focus: null, stress: null, sleepMinutes: null,
  ...overrides,
})

it('sorts chart points chronologically without mutating the source', () => {
  const source = [day('2026-09-02', 7, 0), day('2026-09-01', 6, 0)]
  expect(sortDailySummary(source).map((item) => item.date)).toEqual(['2026-09-01', '2026-09-02'])
  expect(source[0].date).toBe('2026-09-02')
})

it('converts seconds to fractional hours and totals recorded activity', () => {
  const daily = [day('2026-09-01', 6, 5400, { codingSessionCount: 2, githubActivityCount: 3, workoutCount: 1, workoutDurationSeconds: 1800 })]
  expect(secondsToHours(5400)).toBe(1.5)
  expect(totalActivity(daily)).toEqual({ codingSessionCount: 2, codingDurationSeconds: 5400, githubActivityCount: 3, workoutCount: 1, workoutDurationSeconds: 1800 })
})

it('groups recorded days above and at-or-below the median deterministically', () => {
  const result = analyzeProductivity([
    day('2026-09-01', 3, 3600, { energy: 4, focus: 5 }),
    day('2026-09-02', 5, 7200, { energy: 6, focus: 6 }),
    day('2026-09-03', 7, 10800, { energy: 8, focus: 8 }),
    day('2026-09-04', 9, 14400, { energy: 10, focus: 9 }),
    day('2026-09-05', null, 72000, { energy: 10, focus: 10 }),
  ])
  expect(result).toMatchObject({ recordedDayCount: 4, medianProductivity: 6, hasSufficientSample: true })
  expect(result.lower).toMatchObject({ dayCount: 2, averageCodingHours: 1.5, averageEnergy: 5 })
  expect(result.higher).toMatchObject({ dayCount: 2, averageCodingHours: 3.5, averageFocus: 8.5 })
})

it('requires two days in each group and handles missing productivity honestly', () => {
  expect(analyzeProductivity([day('2026-09-01', null, 3600)])).toEqual({ recordedDayCount: 0, medianProductivity: null, higher: null, lower: null, hasSufficientSample: false })
  expect(analyzeProductivity([day('2026-09-01', 5, 0), day('2026-09-02', 5, 0), day('2026-09-03', 8, 0)])).toMatchObject({ hasSufficientSample: false, recordedDayCount: 3 })
})

it('produces a deterministic non-causal summary only for sufficient samples', () => {
  const sufficient = analyzeProductivity([day('a', 2, 3600), day('b', 4, 7200), day('c', 8, 10800), day('d', 10, 14400)])
  expect(describeProductivityPattern(sufficient)).toBe('On higher-productivity recorded days, average coding time was 3.5 hours, compared with 1.5 hours on at-or-below-median days. This is a descriptive comparison, not evidence that either measure caused the other.')
  expect(describeProductivityPattern(analyzeProductivity([]))).toBeNull()
})
