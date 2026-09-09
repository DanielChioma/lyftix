import { API_BASE_URL } from './config'
import { checkInHistoryQueryKey, getCheckInHistory } from './checkIns'
import type { CheckInHistoryParameters } from './checkIns.types'

const parameters: CheckInHistoryParameters = {
  startDate: '2026-09-01', endDate: '2026-09-30', page: 2, size: 20, sortBy: 'focus',
}

afterEach(() => vi.unstubAllGlobals())

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
