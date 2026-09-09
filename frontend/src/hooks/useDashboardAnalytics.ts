import { useQuery } from '@tanstack/react-query'
import { getCheckInAnalytics, getCodingAnalytics, getDailyAnalyticsSummary, getGitHubAnalytics, getWorkoutAnalytics, type AnalyticsDateRange } from '../api/analytics'

const key = (type: string, range: AnalyticsDateRange) => ['analytics', type, range.startDate, range.endDate]

export function useDashboardAnalytics(range: AnalyticsDateRange) {
  return {
    workouts: useQuery({ queryKey: key('workouts', range), queryFn: () => getWorkoutAnalytics(range) }),
    coding: useQuery({ queryKey: key('coding', range), queryFn: () => getCodingAnalytics(range) }),
    github: useQuery({ queryKey: key('github', range), queryFn: () => getGitHubAnalytics(range) }),
    checkIns: useQuery({ queryKey: key('check-ins', range), queryFn: () => getCheckInAnalytics(range) }),
    daily: useQuery({ queryKey: key('daily-summary', range), queryFn: () => getDailyAnalyticsSummary(range) }),
  }
}
