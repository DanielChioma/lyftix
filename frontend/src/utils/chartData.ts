import type { DailyAnalyticsSummary } from '../api/analytics.types'

export interface DashboardChartPoint extends DailyAnalyticsSummary {
  codingHours: number
  workoutHours: number
}

export function transformDailyChartData(daily: DailyAnalyticsSummary[]): DashboardChartPoint[] {
  return [...daily]
    .sort((left, right) => left.date.localeCompare(right.date))
    .map((day) => ({
      ...day,
      codingHours: Number((day.codingDurationSeconds / 3600).toFixed(2)),
      workoutHours: Number((day.workoutDurationSeconds / 3600).toFixed(2)),
    }))
}
