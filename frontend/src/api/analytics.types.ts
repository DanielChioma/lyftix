export interface DateRangeResponse { startDate: string; endDate: string }
export interface WorkoutAnalyticsResponse extends DateRangeResponse {
  totalWorkouts: number; totalCaloriesBurned: number; totalDurationSeconds: number; averageIntensity: number | null
  countsByWorkoutType: Array<{ workoutType: string; count: number }>
  daily: Array<{ date: string; workoutCount: number; caloriesBurned: number; durationSeconds: number }>
}
export interface CodingAnalyticsResponse extends DateRangeResponse {
  totalSessions: number; totalDurationSeconds: number; averageDurationSeconds: number | null
  durationsByProject: Array<{ project: string; durationSeconds: number }>
  durationsByLanguage: Array<{ language: string; durationSeconds: number }>
  daily: Array<{ date: string; sessionCount: number; durationSeconds: number }>
}
export interface GitHubAnalyticsResponse extends DateRangeResponse {
  totalActivities: number
  countsByActivityType: Array<{ activityType: string; count: number }>
  countsByRepository: Array<{ repository: string; count: number }>
  daily: Array<{ date: string; activityCount: number }>
}
export interface CheckInAnalyticsResponse extends DateRangeResponse {
  averageMood: number | null; averageEnergy: number | null; averageFocus: number | null
  averageStress: number | null; averageProductivity: number | null; averageSleepMinutes: number | null
  daily: Array<{ date: string; mood: number; energy: number; focus: number; stress: number; productivity: number; sleepMinutes: number }>
}
export interface DailyAnalyticsSummaryResponse extends DateRangeResponse { daily: DailyAnalyticsSummary[] }
export interface DailyAnalyticsSummary {
  date: string; workoutCount: number; workoutDurationSeconds: number; caloriesBurned: number
  githubActivityCount: number; codingSessionCount: number; codingDurationSeconds: number
  mood: number | null; energy: number | null; focus: number | null; stress: number | null
  productivity: number | null; sleepMinutes: number | null
}
