import { durationBetween, formatAverage, formatDuration } from './format'

it.each([
  [0, '0m'],
  [1800, '30m'],
  [3600, '1h'],
  [5400, '1h 30m'],
])('formats %i seconds as %s', (seconds, expected) => {
  expect(formatDuration(seconds)).toBe(expected)
})

it('derives workout duration from backend timestamps', () => {
  expect(durationBetween('2026-09-09T08:00:00Z', '2026-09-09T09:30:00Z')).toBe(5400)
})

it('preserves missing subjective averages', () => {
  expect(formatAverage(null)).toBe('No data')
  expect(formatAverage(7.25)).toBe('7.3')
})
