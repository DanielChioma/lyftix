import type { WorkoutAnalyticsResponse } from '../api/analytics.types'
import { workoutTrendData, workoutTypeData } from './workoutChartData'

const response: WorkoutAnalyticsResponse = {
  startDate: '2026-09-01', endDate: '2026-09-02', totalWorkouts: 2, totalCaloriesBurned: 700,
  totalDurationSeconds: 5400, averageIntensity: 7,
  countsByWorkoutType: [{ workoutType: 'Running', count: 1 }, { workoutType: 'Cycling', count: 3 }],
  daily: [
    { date: '2026-09-02', workoutCount: 1, caloriesBurned: 400, durationSeconds: 1800 },
    { date: '2026-09-01', workoutCount: 1, caloriesBurned: 300, durationSeconds: 3600 },
  ],
}

it('orders workout trends and converts seconds to minutes', () => {
  expect(workoutTrendData(response)).toMatchObject([
    { date: '2026-09-01', durationMinutes: 60 },
    { date: '2026-09-02', durationMinutes: 30 },
  ])
})

it('orders workout types by descending frequency', () => {
  expect(workoutTypeData(response).map((entry) => entry.workoutType)).toEqual(['Cycling', 'Running'])
})
