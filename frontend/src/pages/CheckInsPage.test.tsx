import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { CheckInsPage } from './CheckInsPage'
import { renderWithProviders } from '../test/renderApp'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null, Legend: () => null,
  Line: ({ name, connectNulls }: { name: string; connectNulls: boolean }) => <span data-connect-nulls={String(connectNulls)}>{name}</span>,
  Bar: ({ name }: { name: string }) => <span>{name}</span>,
}))

const checkIn = {
  id: 1, checkInDate: '2026-09-08', mood: 8, energy: 7, focus: 9, stress: 3,
  sleepMinutes: 435, productivity: 8, notes: 'A deliberately long reflection that must wrap safely.',
  createdAt: '2026-09-08T20:00:00Z', updatedAt: '2026-09-08T20:00:00Z',
}
const analytics = {
  startDate: '2026-08-11', endDate: '2026-09-09', averageMood: 7.25, averageEnergy: 6.5,
  averageFocus: 8, averageStress: 3.5, averageProductivity: 7.75, averageSleepMinutes: 435,
  daily: [
    { date: '2026-09-09', mood: 7, energy: 6, focus: 8, stress: 4, productivity: 7, sleepMinutes: 420 },
    { date: '2026-09-08', mood: 8, energy: 7, focus: 9, stress: 3, productivity: 8, sleepMinutes: 450 },
  ],
}
const page = { content: [checkIn], totalElements: 11, totalPages: 2, size: 10, number: 0, numberOfElements: 1, first: true, last: false, empty: false }
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'check-in-request' } })

function mockRequests(analyticsResponse: unknown = analytics, historyResponse: unknown = page) {
  const request = vi.fn((input: string | URL | Request) => String(input).includes('/api/analytics/check-ins')
    ? Promise.resolve(json(analyticsResponse))
    : String(input).includes('/api/daily-check-ins/filter')
      ? Promise.resolve(json(historyResponse))
      : Promise.reject(new Error(`Unexpected request: ${input}`)))
  vi.stubGlobal('fetch', request)
  return request
}

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers() })

it('renders KPIs, 1–10 scales, charts, summary, and history fields', async () => {
  mockRequests(); renderWithProviders(<CheckInsPage />, '/check-ins')
  expect(await screen.findByLabelText('Average mood (1–10): 7.3 / 10')).toBeInTheDocument()
  expect(screen.getByLabelText('Average sleep: 7h 15m')).toBeInTheDocument()
  expect(await screen.findByText('Subjective trends')).toBeInTheDocument()
  expect(screen.getByText('Sleep duration')).toBeInTheDocument()
  expect(screen.getByLabelText('Period summary')).toHaveTextContent('2 check-ins')
  expect(screen.getAllByText('2026-09-08').length).toBeGreaterThan(0)
  expect(screen.getAllByText('8 / 10').length).toBeGreaterThan(0)
  expect(screen.getAllByText('7h 15m').length).toBeGreaterThan(0)
  expect(screen.getAllByText(/deliberately long reflection/).length).toBeGreaterThan(0)
})

it('uses the default 30-day range and exact inclusive analytics/history parameters', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests()
  renderWithProviders(<CheckInsPage />, '/check-ins'); await screen.findByLabelText('Average mood (1–10): 7.3 / 10')
  expect(request.mock.calls.some(([url]) => String(url).includes('/api/analytics/check-ins?startDate=2026-08-11&endDate=2026-09-09'))).toBe(true)
  expect(request.mock.calls.some(([url]) => String(url).includes('/api/daily-check-ins/filter?startDate=2026-08-11&endDate=2026-09-09&page=0&size=10&sortBy=checkInDate'))).toBe(true)
})

it('updates 7-day, custom, and same-day inclusive ranges', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests(); const view = renderWithProviders(<CheckInsPage />, '/check-ins?page=1')
  fireEvent.change(screen.getByLabelText('Preset'), { target: { value: '7d' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('startDate=2026-09-03&endDate=2026-09-09&page=0'))).toBe(true))
  view.unmount(); renderWithProviders(<CheckInsPage />, '/check-ins?startDate=2026-09-04&endDate=2026-09-04')
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('startDate=2026-09-04&endDate=2026-09-04'))).toBe(true))
})

it('uses backend pagination, resets page for size changes, and allowlists sorting', async () => {
  const request = mockRequests(); renderWithProviders(<CheckInsPage />, '/check-ins?sort=unsafe&page=0')
  fireEvent.click(await screen.findByRole('button', { name: 'Next check-in page' }))
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=1') && String(url).includes('sortBy=checkInDate'))).toBe(true))
  fireEvent.change(screen.getByLabelText('Page size'), { target: { value: '20' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=0') && String(url).includes('size=20'))).toBe(true))
  fireEvent.change(screen.getByLabelText('Sort check-ins'), { target: { value: 'focus' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('sortBy=focus'))).toBe(true))
})

it('shows independent loading states', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined))); renderWithProviders(<CheckInsPage />)
  expect(screen.getByLabelText('Loading Check-in summary')).toBeInTheDocument()
  expect(screen.getByLabelText('Loading check-in history')).toBeInTheDocument()
})

it('shows null KPIs and honest empty analytics/history states', async () => {
  mockRequests({ ...analytics, averageMood: null, averageEnergy: null, averageFocus: null, averageStress: null, averageProductivity: null, averageSleepMinutes: null, daily: [] }, { ...page, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, first: true, last: true, empty: true })
  renderWithProviders(<CheckInsPage />)
  expect(await screen.findByLabelText('Average mood (1–10): No data')).toBeInTheDocument()
  expect(screen.getByLabelText('Average sleep: No data')).toBeInTheDocument()
  expect(screen.getAllByText('No daily check-ins were recorded in this range.').length).toBeGreaterThan(1)
  expect(screen.getByText('No subjective trend data is available for this range.')).toBeInTheDocument()
  expect(screen.getByText('No sleep data is available for this range.')).toBeInTheDocument()
})

it.each([['analytics', 'Analytics unavailable'], ['history', 'History unavailable']])('preserves partial results when %s fails', async (failed, message) => {
  const apiError = { timestamp: '2026-09-09T00:00:00Z', status: 503, error: message, details: ['Try later'] }
  mockRequests()
  vi.mocked(fetch).mockImplementation((input) => {
    if (failed === 'analytics' && String(input).includes('/api/analytics/check-ins')) return Promise.resolve(json(apiError, 503))
    if (failed === 'history' && String(input).includes('/api/daily-check-ins/filter')) return Promise.resolve(json(apiError, 503))
    return String(input).includes('/api/analytics/check-ins') ? Promise.resolve(json(analytics)) : Promise.resolve(json(page))
  })
  renderWithProviders(<CheckInsPage />)
  expect(await screen.findByText(message)).toBeInTheDocument()
  if (failed === 'analytics') expect(screen.getAllByText(/deliberately long reflection/).length).toBeGreaterThan(0)
  else expect(screen.getByLabelText('Average mood (1–10): 7.3 / 10')).toBeInTheDocument()
})
