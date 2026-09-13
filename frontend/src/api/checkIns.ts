import { apiRequest } from './client'
import type { CheckInHistoryParameters, CheckInPage, CreateDailyCheckInRequest, DailyCheckInResponse } from './checkIns.types'

export async function createDailyCheckIn(request: CreateDailyCheckInRequest) {
  return (await apiRequest<DailyCheckInResponse>('/api/daily-check-ins', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })).data
}

export async function getCheckInHistory(parameters: CheckInHistoryParameters) {
  const query = new URLSearchParams({
    startDate: parameters.startDate,
    endDate: parameters.endDate,
    page: String(parameters.page),
    size: String(parameters.size),
    sortBy: parameters.sortBy,
  })
  return (await apiRequest<CheckInPage>(`/api/daily-check-ins/filter?${query}`)).data
}

export const checkInHistoryQueryKey = (parameters: CheckInHistoryParameters) => [
  'check-ins',
  'history',
  parameters.startDate,
  parameters.endDate,
  parameters.page,
  parameters.size,
  parameters.sortBy,
] as const
