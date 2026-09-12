import { apiRequest } from './client'
import type { CreateWorkoutMetricRequest, PageResponse, WorkoutHistoryParameters, WorkoutMetricResponse } from './workouts.types'
import { startOfFollowingUtcDay, startOfUtcDay } from '../utils/dateRange'

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

export async function createWorkout(request: CreateWorkoutMetricRequest) {
  return (await apiRequest<WorkoutMetricResponse>('/api/workouts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })).data
}

export function workoutHistoryQueryKey(parameters: WorkoutHistoryParameters) {
  return ['workouts', 'history', parameters.startDate, parameters.endDate, parameters.page, parameters.size, parameters.sortBy] as const
}
