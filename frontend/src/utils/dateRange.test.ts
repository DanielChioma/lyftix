import { dateRangeFromSearchParams, isValidDateRange, presetDateRange, startOfFollowingUtcDay, startOfUtcDay } from './dateRange'

const today = new Date(2026, 8, 9)

it('defaults to the inclusive last 30 days', () => {
  expect(dateRangeFromSearchParams(new URLSearchParams(), today)).toEqual({
    preset: '30d',
    range: { startDate: '2026-08-11', endDate: '2026-09-09' },
  })
})

it('resolves alternate presets and custom URL state', () => {
  expect(presetDateRange('7d', today)).toEqual({ startDate: '2026-09-03', endDate: '2026-09-09' })
  expect(dateRangeFromSearchParams(new URLSearchParams('startDate=2026-08-01&endDate=2026-08-31'), today)).toEqual({
    preset: 'custom',
    range: { startDate: '2026-08-01', endDate: '2026-08-31' },
  })
})

it('rejects incomplete and reversed custom ranges', () => {
  expect(isValidDateRange({ startDate: '', endDate: '2026-09-09' })).toBe(false)
  expect(isValidDateRange({ startDate: '2026-09-10', endDate: '2026-09-09' })).toBe(false)
})

it('creates half-open UTC timestamp boundaries without relying on end-of-day precision', () => {
  expect(startOfUtcDay('2026-09-01')).toBe('2026-09-01T00:00:00.000Z')
  expect(startOfFollowingUtcDay('2026-09-30')).toBe('2026-10-01T00:00:00.000Z')
})
