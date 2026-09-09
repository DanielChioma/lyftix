import type { PageResponse } from './github.types'

export interface SystemMetricResponse {
  id: number
  hostname: string
  source: string
  cpuPercent: number
  memoryUsedBytes: number
  memoryTotalBytes: number
  diskUsedBytes: number
  diskTotalBytes: number
  loadAverage1m: number | null
  collectedAt: string
  createdAt: string
}

export type SystemMetricSort = 'collectedAt' | 'cpuPercent' | 'loadAverage1m' | 'hostname'

export interface SystemMetricParameters {
  startDate: string
  endDate: string
  hostname: string
  page: number
  size: number
  sortBy: SystemMetricSort
}

export type SystemMetricPage = PageResponse<SystemMetricResponse>
