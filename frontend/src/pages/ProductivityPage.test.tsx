import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { API_BASE_URL } from '../api/config'
import { renderWithProviders } from '../test/renderApp'
import { ProductivityPage } from './ProductivityPage'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  ComposedChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null, Legend: () => null,
  Bar: ({ name }: { name: string }) => <span>{name}</span>, Line: ({ name }: { name: string }) => <span>{name}</span>,
}))

const dailyPoint = (date: string, productivity: number | null) => ({
  date, workoutCount: 1, workoutDurationSeconds: 1800, caloriesBurned: 250,
  githubActivityCount: 3, codingSessionCount: 2, codingDurationSeconds: 5400,
  mood: 7, energy: productivity === null ? null : 8, focus: productivity === null ? null : 9,
  stress: 2, productivity, sleepMinutes: 480,
})
const daily = [dailyPoint('2026-09-01', 3), dailyPoint('2026-09-02', 5), dailyPoint('2026-09-03', 7), dailyPoint('2026-09-04', 9)]
const response = (data: unknown, status = 200) => Promise.resolve(new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } }))

function successfulFetch() {
  return vi.fn((input: RequestInfo | URL) => String(input).includes('/check-ins?')
    ? response({ startDate: '2026-09-01', endDate: '2026-09-04', averageMood: 7, averageEnergy: 7.5, averageFocus: 8.25, averageStress: 2, averageProductivity: 6.75, averageSleepMinutes: 480, daily: [] })
    : response({ startDate: '2026-09-01', endDate: '2026-09-04', daily }))
}

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers() })

it('uses the shared 30-day default range', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const fetchMock = successfulFetch(); vi.stubGlobal('fetch', fetchMock)
  renderWithProviders(<ProductivityPage />, '/productivity')
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/analytics/daily-summary?startDate=2026-08-11&endDate=2026-09-09`, expect.any(Object)))
})

it('requests both analytics resources for the exact custom range and renders KPIs', async () => {
  const fetchMock = successfulFetch(); vi.stubGlobal('fetch', fetchMock)
  renderWithProviders(<ProductivityPage />, '/productivity?startDate=2026-09-01&endDate=2026-09-04')
  expect(await screen.findByLabelText('Average productivity: 6.8')).toBeInTheDocument()
  expect(screen.getByLabelText('Coding time: 6h')).toBeInTheDocument()
  expect(screen.getByLabelText('GitHub activities: 12')).toBeInTheDocument()
  expect(screen.getByLabelText('Workout duration: 2h')).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/analytics/daily-summary?startDate=2026-09-01&endDate=2026-09-04`, expect.any(Object))
  expect(fetchMock).toHaveBeenCalledWith(`${API_BASE_URL}/api/analytics/check-ins?startDate=2026-09-01&endDate=2026-09-04`, expect.any(Object))
})

it('changes both requests when a preset changes', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const fetchMock = successfulFetch(); vi.stubGlobal('fetch', fetchMock)
  renderWithProviders(<ProductivityPage />, '/productivity')
  fireEvent.change(screen.getByLabelText('Preset'), { target: { value: '7d' } })
  await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('startDate=2026-09-03&endDate=2026-09-09'), expect.any(Object)))
})

it('shows loading state without replacing the page shell', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined)))
  renderWithProviders(<ProductivityPage />)
  expect(screen.getByRole('heading', { name: 'Productivity' })).toBeInTheDocument()
  expect(screen.getByLabelText('Loading Daily signals')).toBeInTheDocument()
  expect(screen.getByLabelText('Loading productivity patterns')).toBeInTheDocument()
})

it('preserves null subjective values and reports an insufficient comparison sample', async () => {
  vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => String(input).includes('/check-ins?')
    ? response({ averageProductivity: null, averageFocus: null, averageEnergy: null, daily: [] })
    : response({ daily: [dailyPoint('2026-09-01', null)] })))
  renderWithProviders(<ProductivityPage />)
  expect(await screen.findAllByText('No data')).toHaveLength(3)
  expect(await screen.findByText('At least two recorded days in each group are needed for this comparison.')).toBeInTheDocument()
  expect(screen.getByText('0 recorded days')).toBeInTheDocument()
})

it('keeps daily patterns visible when check-in aggregates fail', async () => {
  vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => String(input).includes('/check-ins?')
    ? response({ timestamp: '2026-09-09T00:00:00Z', status: 503, error: 'Unavailable', details: ['Check-ins unavailable'] }, 503)
    : response({ daily })))
  renderWithProviders(<ProductivityPage />)
  expect(await screen.findByText('Unavailable')).toBeInTheDocument()
  expect(await screen.findByRole('heading', { name: 'Daily activity' })).toBeInTheDocument()
  expect(screen.getByText('4 recorded days')).toBeInTheDocument()
})

it('shows a scoped patterns error when the daily summary fails', async () => {
  vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => String(input).includes('/daily-summary?')
    ? response({ timestamp: '2026-09-09T00:00:00Z', status: 500, error: 'Daily summary unavailable', details: [] }, 500)
    : response({ averageProductivity: 7, averageFocus: 8, averageEnergy: 6, daily: [] })))
  renderWithProviders(<ProductivityPage />)
  expect(await screen.findAllByText('Daily summary unavailable')).toHaveLength(2)
  expect(screen.getByLabelText('Average productivity: 7.0')).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: 'Daily activity' })).not.toBeInTheDocument()
})
