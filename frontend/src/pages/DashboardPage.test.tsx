import { fireEvent, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { ApiClientError } from '../api/client'
import { DashboardPage } from './DashboardPage'
import { renderWithProviders } from '../test/renderApp'
import { useDashboardAnalytics } from '../hooks/useDashboardAnalytics'

vi.mock('../hooks/useDashboardAnalytics')
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null, Legend: () => null,
  Bar: ({ name }: { name: string }) => <span>{name}</span>, Line: ({ name }: { name: string }) => <span>{name}</span>,
}))

const success = (data: unknown) => ({ isPending: false, error: null, data })
const dailyPoint = {
  date: '2026-09-08', workoutCount: 1, workoutDurationSeconds: 3600, caloriesBurned: 400,
  githubActivityCount: 3, codingSessionCount: 2, codingDurationSeconds: 5400,
  mood: 7, energy: 8, focus: 9, stress: 2, productivity: 8, sleepMinutes: 480,
}
const successfulQueries = {
  workouts: success({ totalWorkouts: 4, totalDurationSeconds: 7200, totalCaloriesBurned: 950 }),
  coding: success({ totalSessions: 6, totalDurationSeconds: 12600 }),
  github: success({ totalActivities: 12 }),
  checkIns: success({ averageEnergy: 7.25, averageFocus: 8, averageProductivity: 6.75, daily: [dailyPoint] }),
  daily: success({ daily: [dailyPoint] }),
}

beforeEach(() => vi.mocked(useDashboardAnalytics).mockReturnValue(successfulQueries as ReturnType<typeof useDashboardAnalytics>))

it('renders KPIs from backend-shaped analytics responses', () => {
  renderWithProviders(<DashboardPage />)
  expect(screen.getByLabelText('Workout count: 4')).toBeInTheDocument()
  expect(screen.getByLabelText('Workout duration: 2h')).toBeInTheDocument()
  expect(screen.getByLabelText('Coding duration: 3h 30m')).toBeInTheDocument()
  expect(screen.getByLabelText('GitHub activity: 12')).toBeInTheDocument()
  expect(screen.getByLabelText('Average energy: 7.3')).toBeInTheDocument()
})

it('changes the shared query range when the preset changes', () => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 8, 9))
  renderWithProviders(<DashboardPage />)
  fireEvent.change(screen.getByLabelText('Preset'), { target: { value: '7d' } })
  expect(useDashboardAnalytics).toHaveBeenLastCalledWith({ startDate: '2026-09-03', endDate: '2026-09-09' })
  vi.useRealTimers()
})

it('validates a reversed custom date range before changing queries', () => {
  renderWithProviders(<DashboardPage />, '/?startDate=2026-09-01&endDate=2026-09-09')
  fireEvent.change(screen.getByLabelText('Start date'), { target: { value: '2026-09-10' } })
  fireEvent.click(screen.getByRole('button', { name: 'Apply' }))
  expect(screen.getByRole('alert')).toHaveTextContent('Start date must be on or before end date.')
})

it('shows loading placeholders without replacing the page shell', () => {
  const pending = { isPending: true, error: null, data: undefined }
  vi.mocked(useDashboardAnalytics).mockReturnValue({ workouts: pending, coding: pending, github: pending, checkIns: pending, daily: pending } as ReturnType<typeof useDashboardAnalytics>)
  renderWithProviders(<DashboardPage />)
  expect(screen.getByLabelText('Loading Workouts')).toBeInTheDocument()
  expect(screen.getByLabelText('Loading daily trends')).toBeInTheDocument()
})

it('shows honest empty states and null subjective values', async () => {
  vi.mocked(useDashboardAnalytics).mockReturnValue({
    workouts: success({ totalWorkouts: 0, totalDurationSeconds: 0, totalCaloriesBurned: 0 }),
    coding: success({ totalSessions: 0, totalDurationSeconds: 0 }), github: success({ totalActivities: 0 }),
    checkIns: success({ averageEnergy: null, averageFocus: null, averageProductivity: null, daily: [] }),
    daily: success({ daily: [] }),
  } as unknown as ReturnType<typeof useDashboardAnalytics>)
  renderWithProviders(<DashboardPage />)
  expect(screen.getByText('No workouts recorded.')).toBeInTheDocument()
  expect(screen.getAllByText('No data')).toHaveLength(3)
  expect(await screen.findByText('No check-ins recorded in this range.')).toBeInTheDocument()
})

it('keeps successful sections usable when one API request fails', () => {
  vi.mocked(useDashboardAnalytics).mockReturnValue({
    ...successfulQueries,
    workouts: { isPending: false, data: undefined, error: new ApiClientError('Workout analytics unavailable', 503, 'range-123', null) },
  } as unknown as ReturnType<typeof useDashboardAnalytics>)
  renderWithProviders(<DashboardPage />)
  expect(screen.getByText('Workout analytics unavailable')).toBeInTheDocument()
  expect(screen.getByText('range-123')).toBeInTheDocument()
  expect(screen.getByLabelText('Coding sessions: 6')).toBeInTheDocument()
})
