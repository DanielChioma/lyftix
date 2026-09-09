import type { SystemMetricResponse } from '../api/systemMetrics.types'
import { endOfUtcDayAtPostgresPrecision, formatLoadAverage, formatMetricTimestamp, formatPercentage, metricTrendData, percentage, startOfUtcDay } from './systemMetrics'

const metric = (collectedAt: string, loadAverage1m: number | null = 1.25): SystemMetricResponse => ({ id: 1, hostname: 'host', source: 'psutil', cpuPercent: 25.55, memoryUsedBytes: 400, memoryTotalBytes: 1000, diskUsedBytes: 500, diskTotalBytes: 2000, loadAverage1m, collectedAt, createdAt: collectedAt })

it('builds the exact inclusive timestamp boundaries supported by PostgreSQL', () => {
  expect(startOfUtcDay('2026-09-01')).toBe('2026-09-01T00:00:00.000Z')
  expect(endOfUtcDayAtPostgresPrecision('2026-09-30')).toBe('2026-09-30T23:59:59.999999Z')
})

it('formats percentages and load without turning missing values into zero', () => {
  expect(percentage(400, 1000)).toBe(40)
  expect(percentage(1, 0)).toBeNull()
  expect(formatPercentage(25.55)).toBe('25.6%')
  expect(formatPercentage(null)).toBe('No data')
  expect(formatLoadAverage(1.256)).toBe('1.26')
  expect(formatLoadAverage(null)).toBe('No data')
  expect(formatMetricTimestamp(null)).toBe('No data')
})

it('sorts trends chronologically without mutating samples and derives byte percentages', () => {
  const metrics = [metric('2026-09-02T10:00:00Z'), metric('2026-09-01T10:00:00Z', null)]
  expect(metricTrendData(metrics).map((item) => [item.collectedAt, item.memoryPercent, item.diskPercent, item.loadAverage1m])).toEqual([
    ['2026-09-01T10:00:00Z', 40, 25, null], ['2026-09-02T10:00:00Z', 40, 25, 1.25],
  ])
  expect(metrics[0].collectedAt).toBe('2026-09-02T10:00:00Z')
})
