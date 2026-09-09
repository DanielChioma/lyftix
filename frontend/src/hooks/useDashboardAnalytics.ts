import { useQuery } from '@tanstack/react-query'
import { analyticsQueryKey, getCheckInAnalytics, getCodingAnalytics, getDailyAnalyticsSummary, getGitHubAnalytics, getWorkoutAnalytics, type AnalyticsDateRange } from '../api/analytics'

export function useDashboardAnalytics(range: AnalyticsDateRange) {
  return {
    workouts: useQuery({ queryKey: analyticsQueryKey('workouts', range), queryFn: () => getWorkoutAnalytics(range) }),
    coding: useQuery({ queryKey: analyticsQueryKey('coding', range), queryFn: () => getCodingAnalytics(range) }),
    github: useQuery({ queryKey: analyticsQueryKey('github', range), queryFn: () => getGitHubAnalytics(range) }),
    checkIns: useQuery({ queryKey: analyticsQueryKey('check-ins', range), queryFn: () => getCheckInAnalytics(range) }),
    daily: useQuery({ queryKey: analyticsQueryKey('daily-summary', range), queryFn: () => getDailyAnalyticsSummary(range) }),
  }
}
