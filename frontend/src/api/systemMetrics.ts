import { apiRequest } from './client'
import type { SystemMetricPage, SystemMetricParameters } from './systemMetrics.types'
import { endOfUtcDayAtPostgresPrecision, startOfUtcDay } from '../utils/systemMetrics'

export async function getSystemMetrics(parameters: SystemMetricParameters) {
  const query = new URLSearchParams({
    start: startOfUtcDay(parameters.startDate),
    end: endOfUtcDayAtPostgresPrecision(parameters.endDate),
    page: String(parameters.page),
    size: String(parameters.size),
    sortBy: parameters.sortBy,
  })
  if (parameters.hostname) query.set('hostname', parameters.hostname)
  return (await apiRequest<SystemMetricPage>(`/api/system-metrics/filter?${query}`)).data
}

export const systemMetricsQueryKey = (purpose: 'latest' | 'history', parameters: SystemMetricParameters) => [
  'system-metrics', purpose, parameters.startDate, parameters.endDate, parameters.hostname,
  parameters.page, parameters.size, parameters.sortBy,
] as const
