import { API_BASE_URL } from './config'
import { getCodingAnalytics, getDailyAnalyticsSummary, getWorkoutAnalytics } from './analytics'

const range = { startDate: '2026-08-11', endDate: '2026-09-09' }

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({}), { status: 200 })))
})

afterEach(() => vi.unstubAllGlobals())

it.each([
  [getWorkoutAnalytics, 'workouts'],
  [getCodingAnalytics, 'coding'],
  [getDailyAnalyticsSummary, 'daily-summary'],
])('sends the shared date range to the %s analytics endpoint', async (request, endpoint) => {
  await request(range)
  expect(fetch).toHaveBeenCalledWith(
    `${API_BASE_URL}/api/analytics/${endpoint}?startDate=2026-08-11&endDate=2026-09-09`,
    expect.any(Object),
  )
})
