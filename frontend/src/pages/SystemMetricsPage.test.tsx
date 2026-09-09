import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { SystemMetricsPage } from './SystemMetricsPage'
import { renderWithProviders } from '../test/renderApp'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>, LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null, Legend: () => null,
  Line: ({ name, connectNulls }: { name: string; connectNulls: boolean }) => <span data-connect-nulls={String(connectNulls)}>{name}</span>,
}))

const metric = { id: 9, hostname: 'a-very-long-hostname-that-must-wrap.example', source: 'psutil-worker', cpuPercent: 25.55, memoryUsedBytes: 400, memoryTotalBytes: 1000, diskUsedBytes: 500, diskTotalBytes: 2000, loadAverage1m: 1.256, collectedAt: '2026-09-08T12:00:00Z', createdAt: '2026-09-08T12:00:01Z' }
const page = { content: [metric], totalElements: 11, totalPages: 2, size: 10, number: 0, numberOfElements: 1, first: true, last: false, empty: false }
const latest = { ...page, totalElements: 1, totalPages: 1, size: 1, last: true }
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'system-request' } })

function mockRequests(latestResponse: unknown = latest, historyResponse: unknown = page) {
  const request = vi.fn((input: string | URL | Request) => {
    const url = String(input)
    if (!url.includes('/api/system-metrics/filter')) return Promise.reject(new Error(`Unexpected request: ${input}`))
    const size = new URL(url).searchParams.get('size')
    return Promise.resolve(json(size === '1' ? latestResponse : historyResponse))
  })
  vi.stubGlobal('fetch', request)
  return request
}

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers() })

it('renders latest KPIs, trend context, hostname/source, and history', async () => {
  mockRequests(); renderWithProviders(<SystemMetricsPage />, '/system')
  expect(await screen.findByLabelText('CPU usage: 25.6%')).toBeInTheDocument()
  expect(screen.getByLabelText('Memory usage: 40.0%')).toBeInTheDocument()
  expect(screen.getByLabelText('Disk usage: 25.0%')).toBeInTheDocument()
  expect(screen.getByLabelText('1-minute load average: 1.26')).toBeInTheDocument()
  expect(screen.getAllByText('a-very-long-hostname-that-must-wrap.example').length).toBeGreaterThan(0)
  expect(screen.getAllByText('psutil-worker').length).toBeGreaterThan(0)
  expect(await screen.findByText('Resource usage')).toBeInTheDocument()
  expect(screen.getByText('One-minute load average')).toBeInTheDocument()
})

it('uses the default range and exact timestamp boundaries', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests(); renderWithProviders(<SystemMetricsPage />, '/system')
  await screen.findByLabelText('CPU usage: 25.6%')
  expect(request.mock.calls.some(([url]) => String(url).includes('start=2026-08-11T00%3A00%3A00.000Z') && String(url).includes('end=2026-09-09T23%3A59%3A59.999999Z'))).toBe(true)
})

it('updates 7-day and custom ranges', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests(); const view = renderWithProviders(<SystemMetricsPage />, '/system?page=1')
  fireEvent.change(screen.getByLabelText('Preset'), { target: { value: '7d' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('start=2026-09-03T00%3A00%3A00.000Z') && String(url).includes('page=0'))).toBe(true))
  view.unmount(); renderWithProviders(<SystemMetricsPage />, '/system?startDate=2026-09-01&endDate=2026-09-05')
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('end=2026-09-05T23%3A59%3A59.999999Z'))).toBe(true))
})

it('applies exact hostname filtering and resets pagination', async () => {
  const request = mockRequests(); renderWithProviders(<SystemMetricsPage />, '/system?page=1')
  fireEvent.change(screen.getByLabelText('Hostname'), { target: { value: 'host-one' } }); fireEvent.click(screen.getByRole('button', { name: 'Apply host' }))
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('hostname=host-one') && String(url).includes('page=0'))).toBe(true))
})

it('paginates, resets for page size, and falls back from unsafe sorting', async () => {
  const request = mockRequests(); renderWithProviders(<SystemMetricsPage />, '/system?sort=unsafe')
  fireEvent.click(await screen.findByRole('button', { name: 'Next system-metrics page' }))
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=1') && String(url).includes('sortBy=collectedAt'))).toBe(true))
  fireEvent.change(screen.getByLabelText('Page size'), { target: { value: '20' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=0') && String(url).includes('size=20'))).toBe(true))
  fireEvent.change(screen.getByLabelText('Sort metrics'), { target: { value: 'cpuPercent' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('sortBy=cpuPercent'))).toBe(true))
})

it('shows section loading and honest empty states', async () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined))); const view = renderWithProviders(<SystemMetricsPage />)
  expect(screen.getByLabelText('Loading Latest collected sample')).toBeInTheDocument(); expect(screen.getByLabelText('Loading system metric trends')).toBeInTheDocument(); expect(screen.getByLabelText('Loading system-metrics history')).toBeInTheDocument()
  view.unmount(); mockRequests({ ...latest, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, empty: true }, { ...page, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, first: true, last: true, empty: true }); renderWithProviders(<SystemMetricsPage />)
  expect(await screen.findAllByText('No system metrics were recorded in this range.')).toHaveLength(3)
  expect(screen.getByText('No load-average samples are available for this range.')).toBeInTheDocument()
})

it('preserves ApiErrorResponse and correlation-ID presentation', async () => {
  const error = { timestamp: '2026-09-09T00:00:00Z', status: 503, error: 'Metrics unavailable', details: ['Try later'] }
  mockRequests(error, error); vi.mocked(fetch).mockImplementation((input) => Promise.resolve(json(error, String(input).includes('/api/system-metrics/filter') ? 503 : 200)))
  renderWithProviders(<SystemMetricsPage />)
  expect((await screen.findAllByText('Metrics unavailable')).length).toBeGreaterThan(1)
  expect(screen.getAllByText(/system-request/).length).toBeGreaterThan(1)
})
