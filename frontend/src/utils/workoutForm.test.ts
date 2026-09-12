import { localDateTimeToInstant, toLocalDateTimeValue } from './workoutForm'

it('converts a local datetime to the corresponding UTC instant', () => {
  expect(localDateTimeToInstant('2026-01-15T10:30')).toBe('2026-01-15T10:30:00.000Z')
})

it('formats a Date for a datetime-local input without changing its displayed local time', () => {
  expect(toLocalDateTimeValue(new Date('2026-01-15T10:30:00Z'))).toBe('2026-01-15T10:30')
})
