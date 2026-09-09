import { API_BASE_URL } from './config'
import { getWorkoutHistory, workoutHistoryQueryKey } from './workouts'

const parameters = { startDate: '2026-09-01', endDate: '2026-09-07', page: 2, size: 20, sortBy: 'intensity' as const }

afterEach(() => vi.unstubAllGlobals())

it('sends a start-inclusive and end-exclusive UTC range with exact paging and sort parameters', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ content: [] }), { status: 200 })))
  await getWorkoutHistory(parameters)
  expect(fetch).toHaveBeenCalledWith(
    `${API_BASE_URL}/api/workouts/filter?start=2026-09-01T00%3A00%3A00.000Z&end=2026-09-08T00%3A00%3A00.000Z&page=2&size=20&sortBy=intensity`,
    expect.any(Object),
  )
})

it('keys history by range and all server paging controls', () => {
  expect(workoutHistoryQueryKey(parameters)).toEqual([
    'workouts', 'history', '2026-09-01', '2026-09-07', 2, 20, 'intensity',
  ])
})
