import { fireEvent, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { GitHubPage } from './GitHubPage'
import { renderWithProviders } from '../test/renderApp'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: ReactNode }) => <div>{children}</div>, BarChart: ({ children }: { children: ReactNode }) => <div>{children}</div>, LineChart: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  CartesianGrid: () => null, XAxis: () => null, YAxis: () => null, Tooltip: () => null,
  Bar: ({ name }: { name: string }) => <span>{name}</span>, Line: ({ name }: { name: string }) => <span>{name}</span>,
}))

const activity = { id: 42, activityType: 'PULL_REQUEST_MERGED', repositoryName: 'a-very-long-repository-name-that-must-wrap', repositoryOwner: 'octocat', occurredAt: '2026-09-08T14:30:00Z', externalId: 'hidden-id', title: 'Merge an intentionally long pull request title that remains readable on mobile', createdAt: '2026-09-08T14:31:00Z', updatedAt: '2026-09-08T14:31:00Z' }
const analytics = { startDate: '2026-08-11', endDate: '2026-09-09', totalActivities: 8, countsByActivityType: [{ activityType: 'PushEvent', count: 3 }, { activityType: 'PULL_REQUEST_MERGED', count: 5 }], countsByRepository: [{ repository: 'octocat/secondary', count: 3 }, { repository: 'octocat/lyftix', count: 5 }], daily: [{ date: '2026-09-09', activityCount: 3 }, { date: '2026-09-08', activityCount: 5 }] }
const page = { content: [activity], totalElements: 11, totalPages: 2, size: 10, number: 0, numberOfElements: 1, first: true, last: false, empty: false }
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), { status, headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'github-request' } })

function mockRequests(analyticsResponse: unknown = analytics, historyResponse: unknown = page) {
  const request = vi.fn((input: string | URL | Request) => String(input).includes('/api/analytics/github') ? Promise.resolve(json(analyticsResponse)) : String(input).includes('/api/github-activities/filter') ? Promise.resolve(json(historyResponse)) : Promise.reject(new Error(`Unexpected request: ${input}`)))
  vi.stubGlobal('fetch', request); return request
}

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers() })

it('renders backend analytics KPIs, ranked data, and history fields', async () => {
  mockRequests(); renderWithProviders(<GitHubPage />, '/github')
  expect(await screen.findByLabelText('Total activities: 8')).toBeInTheDocument()
  expect(screen.getByLabelText('Active repositories: 2')).toBeInTheDocument()
  expect(screen.getByLabelText('Most active repository: octocat/lyftix')).toBeInTheDocument()
  expect(screen.getByLabelText('Most common activity: Pull request merged')).toBeInTheDocument()
  expect(screen.getAllByText('Pull request merged').length).toBeGreaterThan(0)
  expect(screen.getAllByText('octocat/a-very-long-repository-name-that-must-wrap').length).toBeGreaterThan(0)
  expect(screen.getAllByText(/Merge an intentionally long/).length).toBeGreaterThan(0)
  expect(screen.queryByText('hidden-id')).not.toBeInTheDocument()
})

it('uses the default date range and exact half-open history boundary', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests(); renderWithProviders(<GitHubPage />, '/github')
  await screen.findByLabelText('Total activities: 8')
  expect(request.mock.calls.some(([url]) => String(url).includes('/api/analytics/github?startDate=2026-08-11&endDate=2026-09-09'))).toBe(true)
  expect(request.mock.calls.some(([url]) => String(url).includes('start=2026-08-11T00%3A00%3A00.000Z') && String(url).includes('end=2026-09-10T00%3A00%3A00.000Z'))).toBe(true)
})

it('updates preset and custom date-range requests', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true }); vi.setSystemTime(new Date(2026, 8, 9)); const request = mockRequests(); const view = renderWithProviders(<GitHubPage />, '/github')
  fireEvent.change(screen.getByLabelText('Preset'), { target: { value: '7d' } })
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('startDate=2026-09-03&endDate=2026-09-09'))).toBe(true))
  view.unmount(); renderWithProviders(<GitHubPage />, '/github?startDate=2026-09-01&endDate=2026-09-07')
  await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('start=2026-09-01T00%3A00%3A00.000Z') && String(url).includes('end=2026-09-08T00%3A00%3A00.000Z'))).toBe(true))
})

it('uses backend pagination and only allowlisted sorting', async () => {
  const request = mockRequests(); renderWithProviders(<GitHubPage />, '/github?sort=unsafeField')
  const next = await screen.findByRole('button', { name: 'Next GitHub activity page' }); expect(request.mock.calls.some(([url]) => String(url).includes('sortBy=occurredAt'))).toBe(true)
  fireEvent.click(next); await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=1'))).toBe(true))
  fireEvent.change(screen.getByLabelText('Sort activities'), { target: { value: 'repositoryName' } }); await waitFor(() => expect(request.mock.calls.some(([url]) => String(url).includes('page=0') && String(url).includes('sortBy=repositoryName'))).toBe(true))
})

it('renders independent loading states', () => {
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => undefined))); renderWithProviders(<GitHubPage />, '/github')
  expect(screen.getByLabelText('Loading GitHub summary')).toBeInTheDocument(); expect(screen.getByLabelText('Loading GitHub activity history')).toBeInTheDocument()
})

it('renders honest empty analytics and history states', async () => {
  mockRequests({ ...analytics, totalActivities: 0, countsByActivityType: [], countsByRepository: [], daily: [] }, { ...page, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, first: true, last: true, empty: true }); renderWithProviders(<GitHubPage />, '/github')
  expect(await screen.findByLabelText('Most active repository: No data')).toBeInTheDocument(); expect(screen.getByLabelText('Most common activity: No data')).toBeInTheDocument(); expect(screen.getAllByText('No GitHub activity was recorded in this range.').length).toBeGreaterThan(1); expect(screen.getByText('No repository activity is available for this range.')).toBeInTheDocument()
})

it.each([
  ['analytics', 'Analytics unavailable'],
  ['history', 'History unavailable'],
])('preserves partial results when %s fails', async (failed, error) => {
  const apiError = { timestamp: '2026-09-09T00:00:00Z', status: 503, error, details: ['Try later'] }
  mockRequests()
  vi.mocked(fetch).mockImplementation((input) => {
    if (failed === 'analytics' && String(input).includes('/api/analytics/github')) return Promise.resolve(json(apiError, 503))
    if (failed === 'history' && String(input).includes('/api/github-activities/filter')) return Promise.resolve(json(apiError, 503))
    return String(input).includes('/api/analytics/github') ? Promise.resolve(json(analytics)) : Promise.resolve(json(page))
  })
  renderWithProviders(<GitHubPage />, '/github')
  expect(await screen.findByText(error)).toBeInTheDocument()
  if (failed === 'analytics') expect(screen.getAllByText(/Merge an intentionally long/).length).toBeGreaterThan(0)
  else expect(screen.getByLabelText('Total activities: 8')).toBeInTheDocument()
})
