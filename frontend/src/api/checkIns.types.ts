import type { PageResponse } from './github.types'

export interface DailyCheckInResponse {
  id: number
  checkInDate: string
  mood: number
  energy: number
  focus: number
  stress: number
  sleepMinutes: number
  productivity: number
  notes: string | null
  createdAt: string
  updatedAt: string
}

export type CheckInSort =
  | 'checkInDate'
  | 'mood'
  | 'energy'
  | 'focus'
  | 'stress'
  | 'productivity'
  | 'sleepMinutes'

export interface CheckInHistoryParameters {
  startDate: string
  endDate: string
  page: number
  size: number
  sortBy: CheckInSort
}

export type CheckInPage = PageResponse<DailyCheckInResponse>
