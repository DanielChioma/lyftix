import type { SystemMetricResponse } from '../api/systemMetrics.types'

export function startOfUtcDay(date: string) { return `${date}T00:00:00.000Z` }

export function endOfUtcDayAtPostgresPrecision(date: string) {
  return `${date}T23:59:59.999999Z`
}

export function percentage(used: number, total: number) {
  return total > 0 ? used / total * 100 : null
}

export function formatPercentage(value: number | null) {
  return value === null ? 'No data' : `${value.toFixed(1)}%`
}

export function formatLoadAverage(value: number | null) {
  return value === null ? 'No data' : value.toFixed(2)
}

export function formatMetricTimestamp(value: string | null) {
  return value === null ? 'No data' : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function sortMetricsChronologically(metrics: SystemMetricResponse[]) {
  return [...metrics].sort((a, b) => a.collectedAt.localeCompare(b.collectedAt))
}

export function metricTrendData(metrics: SystemMetricResponse[]) {
  return sortMetricsChronologically(metrics).map((metric) => ({
    ...metric,
    memoryPercent: percentage(metric.memoryUsedBytes, metric.memoryTotalBytes),
    diskPercent: percentage(metric.diskUsedBytes, metric.diskTotalBytes),
  }))
}
