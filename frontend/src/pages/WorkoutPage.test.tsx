import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { WorkoutPage } from './WorkoutPage'
import { renderWithProviders } from '../test/renderApp'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null, Legend: () => null,
  Bar: ({ name }: { name: string }) => <span>{name}</span>, Line: ({ name }: { name: string }) => <span>{name}</span>,
}))

const workout = {
  id: 42, workoutType: 'Running', intensity: 8, caloriesBurned: 450,
  startedAt: '2026-09-08T08:00:00Z', endedAt: '2026-09-08T09:30:00Z',
  createdAt: '2026-09-08T09:30:01Z', updatedAt: '2026-09-08T09:30:01Z',
}
const analytics = {
  startDate: '2026-08-11', endDate: '2026-09-09', totalWorkouts: 3, totalCaloriesBurned: 1250,
  totalDurationSeconds: 9000, averageIntensity: 7.5,
  countsByWorkoutType: [{ workoutType: 'Running', count: 3 }],
  daily: [{ date: '2026-09-08', workoutCount: 3, caloriesBurned: 1250, durationSeconds: 9000 }],
}
const page = {
  content: [workout], totalElements: 11, totalPages: 2, size: 10, number: 0,
  numberOfElements: 1, first: true, last: false, empty: false,
}

function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'workout-request' } })
}

function mockRequests(workoutAnalytics: unknown = analytics, history: unknown = page) {
  const request = vi.fn((input: string | URL | Request) => {
    const url = String(input)
    if (url.includes('/api/analytics/workouts')) return Promise.resolve(json(workoutAnalytics))
    if (url.includes('/api/workouts/filter')) return Promise.resolve(json(history))
    return Promise.reject(new Error(`Unexpected request: ${url}`))
  })
  vi.stubGlobal('fetch', request)
  return request
}

afterEach(() => vi.unstubAllGlobals())

it('renders workout analytics KPIs and real history fields', async () => {
  mockRequests()
  renderWithProviders(<WorkoutPage />, '/workouts')
  expect(await screen.findByLabelText('Total workouts: 3')).toBeInTheDocument()
  expect(screen.getByLabelText('Total duration: 2h 30m')).toBeInTheDocument()
  expect(screen.getByLabelText('Calories burned: 1,250')).toBeInTheDocument()
  expect(screen.getByLabelText('Average intensity: 7.5')).toBeInTheDocument()
  expect(screen.getAllByText('Running').length).toBeGreaterThan(0)
  expect(screen.getAllByText('1h 30m').length).toBeGreaterThan(0)
})

it('preserves a null average intensity as No data', async () => {
  mockRequests({ ...analytics, totalWorkouts: 0, averageIntensity: null, daily: [], countsByWorkoutType: [] }, { ...page, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, first: true, last: true, empty: true })
  renderWithProviders(<WorkoutPage />, '/workouts')
  expect(await screen.findByLabelText('Average intensity: No data')).toBeInTheDocument()
  expect(screen.getByText('No workouts recorded in this date range.')).toBeInTheDocument()
})

it('requests the custom date filter and server sort exactly', async () => {
  const request = mockRequests()
  renderWithProviders(<WorkoutPage />, '/workouts?startDate=2026-09-01&endDate=2026-09-07&sort=intensity')
  await screen.findByLabelText('Total workouts: 3')
  expect(request.mock.calls.some(([url]) => String(url).includes('start=2026-09-01T00%3A00%3A00.000Z') && String(url).includes('end=2026-09-08T00%3A00%3A00.000Z') && String(url).includes('sortBy=intensity'))).toBe(true)
})

it('uses backend pagination and updates the requested page', async () => {
  const request = mockRequests()
  renderWithProviders(<WorkoutPage />, '/workouts')
  const next = await screen.findByRole('button', { name: 'Next workout page' })
  fireEvent.click(next)
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('/api/workouts/filter?') && String(url).includes('page=1'))).toBe(true))
  expect(screen.getByText('Page 1 of 2')).toBeInTheDocument()
})

it('resets pagination when sort changes and sends only an allowed sort', async () => {
  const request = mockRequests()
  renderWithProviders(<WorkoutPage />, '/workouts?page=1')
  fireEvent.change(await screen.findByLabelText('Sort workouts'), { target: { value: 'caloriesBurned' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=0') && String(url).includes('sortBy=caloriesBurned'))).toBe(true))
})

it('renders independent loading states', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined)))
  renderWithProviders(<WorkoutPage />, '/workouts')
  expect(screen.getByLabelText('Loading Workout summary')).toBeInTheDocument()
  expect(screen.getByLabelText('Loading workout history')).toBeInTheDocument()
})

it('keeps history visible when analytics fails', async () => {
  const error = { timestamp: '2026-09-09T10:00:00Z', status: 503, error: 'Analytics unavailable', details: ['Try again later'] }
  mockRequests(error, page)
  vi.mocked(fetch).mockImplementation((input) => String(input).includes('/api/analytics/workouts') ? Promise.resolve(json(error, 503)) : Promise.resolve(json(page)))
  renderWithProviders(<WorkoutPage />, '/workouts')
  expect(await screen.findByText('Analytics unavailable')).toBeInTheDocument()
  expect(screen.getAllByText('Running').length).toBeGreaterThan(0)
})

it('keeps analytics visible when history fails', async () => {
  const error = { timestamp: '2026-09-09T10:00:00Z', status: 500, error: 'History unavailable', details: [] }
  mockRequests()
  vi.mocked(fetch).mockImplementation((input) => String(input).includes('/api/workouts/filter') ? Promise.resolve(json(error, 500)) : Promise.resolve(json(analytics)))
  renderWithProviders(<WorkoutPage />, '/workouts')
  expect(await screen.findByText('History unavailable')).toBeInTheDocument()
  expect(screen.getByLabelText('Total workouts: 3')).toBeInTheDocument()
})
