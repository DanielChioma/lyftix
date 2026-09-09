export interface WorkoutMetricResponse {
  id: number
  workoutType: string
  intensity: number
  caloriesBurned: number
  startedAt: string
  endedAt: string
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

export type WorkoutSort = 'startedAt' | 'caloriesBurned' | 'intensity'

export interface WorkoutHistoryParameters {
  startDate: string
  endDate: string
  page: number
  size: number
  sortBy: WorkoutSort
}
