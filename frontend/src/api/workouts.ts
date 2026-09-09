import { apiRequest } from './client'
import type { PageResponse, WorkoutHistoryParameters, WorkoutMetricResponse } from './workouts.types'

function startOfUtcDay(date: string) { return `${date}T00:00:00.000Z` }

function startOfFollowingUtcDay(date: string) {
  const [year, month, day] = date.split('-').map(Number)
  const followingDay = new Date(Date.UTC(year, month - 1, day + 1))
  return `${followingDay.toISOString().slice(0, 10)}T00:00:00.000Z`
}

export async function getWorkoutHistory(parameters: WorkoutHistoryParameters) {
  const query = new URLSearchParams({
    start: startOfUtcDay(parameters.startDate),
    end: startOfFollowingUtcDay(parameters.endDate),
    page: String(parameters.page),
    size: String(parameters.size),
    sortBy: parameters.sortBy,
  })
  return (await apiRequest<PageResponse<WorkoutMetricResponse>>(`/api/workouts/filter?${query.toString()}`)).data
}

export function workoutHistoryQueryKey(parameters: WorkoutHistoryParameters) {
  return ['workouts', 'history', parameters.startDate, parameters.endDate, parameters.page, parameters.size, parameters.sortBy] as const
}
