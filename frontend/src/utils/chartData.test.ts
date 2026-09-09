import type { DailyAnalyticsSummary } from '../api/analytics.types'
import { transformDailyChartData } from './chartData'

const day = (date: string, codingDurationSeconds: number, workoutDurationSeconds: number): DailyAnalyticsSummary => ({
  date, codingDurationSeconds, workoutDurationSeconds, workoutCount: 0, caloriesBurned: 0,
  githubActivityCount: 0, codingSessionCount: 0, mood: null, energy: null, focus: null,
  stress: null, productivity: null, sleepMinutes: null,
})

it('sorts daily chart points chronologically and converts seconds to hours', () => {
  expect(transformDailyChartData([day('2026-09-02', 1800, 7200), day('2026-09-01', 3600, 0)]))
    .toMatchObject([
      { date: '2026-09-01', codingHours: 1, workoutHours: 0 },
      { date: '2026-09-02', codingHours: 0.5, workoutHours: 2 },
    ])
})
