import { toLocalDateValue } from './checkInForm'

it('formats the user local calendar date without converting it to UTC', () => {
  expect(toLocalDateValue(new Date(2026, 8, 13, 0, 30))).toBe('2026-09-13')
})
