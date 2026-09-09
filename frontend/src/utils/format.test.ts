import { formatAverage, formatDuration } from './format'

it.each([
  [0, '0m'],
  [1800, '30m'],
  [3600, '1h'],
  [5400, '1h 30m'],
])('formats %i seconds as %s', (seconds, expected) => {
  expect(formatDuration(seconds)).toBe(expected)
})

it('preserves missing subjective averages', () => {
  expect(formatAverage(null)).toBe('No data')
  expect(formatAverage(7.25)).toBe('7.3')
})
