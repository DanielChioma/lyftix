import type { WorkoutAnalyticsResponse } from '../api/analytics.types'

export function workoutTrendData(response: WorkoutAnalyticsResponse) {
  return [...response.daily]
    .sort((left, right) => left.date.localeCompare(right.date))
    .map((day) => ({
      ...day,
      caloriesBurned: day.workoutsWithCalories > 0 ? day.caloriesBurned : null,
      durationMinutes: Number((day.durationSeconds / 60).toFixed(1)),
    }))
}

export function workoutTypeData(response: WorkoutAnalyticsResponse) {
  return [...response.countsByWorkoutType].sort((left, right) => right.count - left.count)
}
