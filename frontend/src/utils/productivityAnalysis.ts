import type { DailyAnalyticsSummary } from '../api/analytics.types'

export interface ActivityTotals {
  codingSessionCount: number
  codingDurationSeconds: number
  githubActivityCount: number
  workoutCount: number
  workoutDurationSeconds: number
}

export interface ProductivityGroup {
  dayCount: number
  averageCodingHours: number
  averageWorkoutHours: number
  averageGitHubActivities: number
  averageEnergy: number | null
  averageFocus: number | null
}

export interface ProductivityAnalysis {
  recordedDayCount: number
  medianProductivity: number | null
  higher: ProductivityGroup | null
  lower: ProductivityGroup | null
  hasSufficientSample: boolean
}

export const secondsToHours = (seconds: number) => seconds / 3600

export function sortDailySummary(daily: DailyAnalyticsSummary[]) {
  return [...daily].sort((left, right) => left.date.localeCompare(right.date))
}

export function totalActivity(daily: DailyAnalyticsSummary[]): ActivityTotals {
  return daily.reduce((totals, day) => ({
    codingSessionCount: totals.codingSessionCount + day.codingSessionCount,
    codingDurationSeconds: totals.codingDurationSeconds + day.codingDurationSeconds,
    githubActivityCount: totals.githubActivityCount + day.githubActivityCount,
    workoutCount: totals.workoutCount + day.workoutCount,
    workoutDurationSeconds: totals.workoutDurationSeconds + day.workoutDurationSeconds,
  }), { codingSessionCount: 0, codingDurationSeconds: 0, githubActivityCount: 0, workoutCount: 0, workoutDurationSeconds: 0 })
}

function average(values: number[]) {
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function nullableAverage(values: Array<number | null>) {
  const recorded = values.filter((value): value is number => value !== null)
  return recorded.length === 0 ? null : average(recorded)
}

function summarizeGroup(days: DailyAnalyticsSummary[]): ProductivityGroup {
  return {
    dayCount: days.length,
    averageCodingHours: average(days.map((day) => secondsToHours(day.codingDurationSeconds))),
    averageWorkoutHours: average(days.map((day) => secondsToHours(day.workoutDurationSeconds))),
    averageGitHubActivities: average(days.map((day) => day.githubActivityCount)),
    averageEnergy: nullableAverage(days.map((day) => day.energy)),
    averageFocus: nullableAverage(days.map((day) => day.focus)),
  }
}

export function analyzeProductivity(daily: DailyAnalyticsSummary[]): ProductivityAnalysis {
  const recordedDays = daily.filter((day) => day.productivity !== null)
  if (recordedDays.length === 0) {
    return { recordedDayCount: 0, medianProductivity: null, higher: null, lower: null, hasSufficientSample: false }
  }

  const values = recordedDays.map((day) => day.productivity as number).sort((a, b) => a - b)
  const middle = Math.floor(values.length / 2)
  const median = values.length % 2 === 0 ? (values[middle - 1] + values[middle]) / 2 : values[middle]
  const higherDays = recordedDays.filter((day) => (day.productivity as number) > median)
  const lowerDays = recordedDays.filter((day) => (day.productivity as number) <= median)
  const sufficient = higherDays.length >= 2 && lowerDays.length >= 2

  return {
    recordedDayCount: recordedDays.length,
    medianProductivity: median,
    higher: higherDays.length === 0 ? null : summarizeGroup(higherDays),
    lower: lowerDays.length === 0 ? null : summarizeGroup(lowerDays),
    hasSufficientSample: sufficient,
  }
}

export function describeProductivityPattern(analysis: ProductivityAnalysis) {
  if (!analysis.hasSufficientSample || !analysis.higher || !analysis.lower) return null
  return `On higher-productivity recorded days, average coding time was ${analysis.higher.averageCodingHours.toFixed(1)} hours, compared with ${analysis.lower.averageCodingHours.toFixed(1)} hours on at-or-below-median days. This is a descriptive comparison, not evidence that either measure caused the other.`
}
