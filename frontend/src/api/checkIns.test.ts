import { API_BASE_URL } from './config'
import { resetCsrfToken } from './client'
import { checkInHistoryQueryKey, createDailyCheckIn, getCheckInHistory } from './checkIns'
import type { CheckInHistoryParameters } from './checkIns.types'

const parameters: CheckInHistoryParameters = {
  startDate: '2026-09-01', endDate: '2026-09-30', page: 2, size: 20, sortBy: 'focus',
}

afterEach(() => { resetCsrfToken(); vi.unstubAllGlobals() })

it('posts the exact create payload with an unchanged LocalDate', async () => {
  const request = {
    checkInDate: '2026-09-13', mood: 8, energy: 7, focus: 9, stress: 3,
    productivity: 8, sleepMinutes: 465, notes: null,
  }
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ headerName: 'X-XSRF-TOKEN', parameterName: '_csrf', token: 'token' }), { status: 200 }))
    .mockResolvedValueOnce(new Response(JSON.stringify({ id: 1, ...request }), { status: 201, headers: { 'Content-Type': 'application/json' } }))
  vi.stubGlobal('fetch', fetchMock)
  await createDailyCheckIn(request)
  expect(fetchMock).toHaveBeenNthCalledWith(2, `${API_BASE_URL}/api/daily-check-ins`, expect.objectContaining({
    method: 'POST', body: JSON.stringify(request), credentials: 'include',
  }))
})

it('sends exact inclusive LocalDate history parameters', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{}', { status: 200 })))
  await getCheckInHistory(parameters)
  expect(fetch).toHaveBeenCalledWith(
    `${API_BASE_URL}/api/daily-check-ins/filter?startDate=2026-09-01&endDate=2026-09-30&page=2&size=20&sortBy=focus`,
    expect.any(Object),
  )
})

it('creates a stable history query key from every paging input', () => {
  expect(checkInHistoryQueryKey(parameters)).toEqual(['check-ins', 'history', '2026-09-01', '2026-09-30', 2, 20, 'focus'])
})
