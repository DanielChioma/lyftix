import { apiRequest } from './client'
import type { CheckInAnalyticsResponse, CodingAnalyticsResponse, DailyAnalyticsSummaryResponse, GitHubAnalyticsResponse, WorkoutAnalyticsResponse } from './analytics.types'

export interface AnalyticsDateRange { startDate: string; endDate: string }

export const analyticsQueryKey = (type: string, range: AnalyticsDateRange) => [
  'analytics', type, range.startDate, range.endDate,
] as const

function analyticsPath(endpoint: string, range: AnalyticsDateRange) {
  const params = new URLSearchParams({ startDate: range.startDate, endDate: range.endDate })
  return `/api/analytics/${endpoint}?${params.toString()}`
}

export async function getWorkoutAnalytics(range: AnalyticsDateRange) {
  return (await apiRequest<WorkoutAnalyticsResponse>(analyticsPath('workouts', range))).data
}
export async function getCodingAnalytics(range: AnalyticsDateRange) {
  return (await apiRequest<CodingAnalyticsResponse>(analyticsPath('coding', range))).data
}
export async function getGitHubAnalytics(range: AnalyticsDateRange) {
  return (await apiRequest<GitHubAnalyticsResponse>(analyticsPath('github', range))).data
}
export async function getCheckInAnalytics(range: AnalyticsDateRange) {
  return (await apiRequest<CheckInAnalyticsResponse>(analyticsPath('check-ins', range))).data
}
export async function getDailyAnalyticsSummary(range: AnalyticsDateRange) {
  return (await apiRequest<DailyAnalyticsSummaryResponse>(analyticsPath('daily-summary', range))).data
}
