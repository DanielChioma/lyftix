import { API_BASE_URL } from './config'
import { createWorkout, getWorkoutHistory, workoutHistoryQueryKey } from './workouts'
import { resetCsrfToken } from './client'

const parameters = { startDate: '2026-09-01', endDate: '2026-09-07', page: 2, size: 20, sortBy: 'intensity' as const }

afterEach(() => { resetCsrfToken(); vi.unstubAllGlobals() })

it('posts the exact typed workout payload', async () => {
  const request = {
    workoutType: 'Cycling', intensity: 6, caloriesBurned: 320,
    startedAt: '2026-09-11T08:15:00.000Z', endedAt: '2026-09-11T09:00:00.000Z',
  }
  const response = { id: 7, ...request, createdAt: '2026-09-11T09:01:00Z', updatedAt: '2026-09-11T09:01:00Z' }
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'token' }), { status: 200 }))
    .mockResolvedValueOnce(new Response(JSON.stringify(response), { status: 201 }))
  vi.stubGlobal('fetch', fetchMock)

  await expect(createWorkout(request)).resolves.toEqual(response)
  expect(fetchMock).toHaveBeenNthCalledWith(2, `${API_BASE_URL}/api/workouts`, expect.objectContaining({
    method: 'POST', body: JSON.stringify(request), credentials: 'include',
  }))
})

it('preserves a structured create-workout API failure and correlation id', async () => {
  const request = {
    workoutType: 'Cycling', intensity: 6, caloriesBurned: 320,
    startedAt: '2026-09-11T08:15:00.000Z', endedAt: '2026-09-11T09:00:00.000Z',
  }
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'token' }), { status: 200 }))
    .mockResolvedValueOnce(new Response(JSON.stringify({
      timestamp: '2026-09-11T09:00:00Z', status: 400, error: 'Validation Failed', details: ['Workout data was rejected'],
    }), { status: 400, headers: { 'Content-Type': 'application/json', 'X-Correlation-ID': 'request-42' } }))
  vi.stubGlobal('fetch', fetchMock)

  await expect(createWorkout(request)).rejects.toMatchObject({
    status: 400, correlationId: 'request-42', response: { error: 'Validation Failed', details: ['Workout data was rejected'] },
  })
})

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
