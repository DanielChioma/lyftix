import { API_BASE_URL } from './config'
import { getSystemMetrics, systemMetricsQueryKey } from './systemMetrics'
import type { SystemMetricParameters } from './systemMetrics.types'

const parameters: SystemMetricParameters = { startDate: '2026-09-01', endDate: '2026-09-30', hostname: 'host one', page: 2, size: 20, sortBy: 'cpuPercent' }

afterEach(() => vi.unstubAllGlobals())

it('sends exact inclusive PostgreSQL-precision timestamp and hostname parameters', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 200 })))
  await getSystemMetrics(parameters)
  expect(fetch).toHaveBeenCalledWith(
    `${API_BASE_URL}/api/system-metrics/filter?start=2026-09-01T00%3A00%3A00.000Z&end=2026-09-30T23%3A59%3A59.999999Z&page=2&size=20&sortBy=cpuPercent&hostname=host+one`,
    expect.any(Object),
  )
})

it('includes all filter and paging state in query keys', () => {
  expect(systemMetricsQueryKey('history', parameters)).toEqual(['system-metrics', 'history', '2026-09-01', '2026-09-30', 'host one', 2, 20, 'cpuPercent'])
})
