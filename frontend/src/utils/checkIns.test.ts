import { formatSleepMinutes, formatSubjectiveAverage, sleepTrendData, subjectiveTrendData } from './checkIns'

const daily = [
  { date: '2026-09-03', mood: 8, energy: 7, focus: 9, stress: 3, productivity: 8, sleepMinutes: 480 },
  { date: '2026-09-01', mood: 6, energy: 5, focus: 7, stress: 4, productivity: 6, sleepMinutes: 435 },
]

it('formats subjective averages without converting null to zero', () => {
  expect(formatSubjectiveAverage(7.25)).toBe('7.3 / 10')
  expect(formatSubjectiveAverage(null)).toBe('No data')
})

it('formats sleep minutes as readable hours and minutes', () => {
  expect(formatSleepMinutes(435)).toBe('7h 15m')
  expect(formatSleepMinutes(60)).toBe('1h')
  expect(formatSleepMinutes(null)).toBe('No data')
})

it('sorts subjective trends chronologically without mutating input', () => {
  const original = daily.map((point) => point.date)
  expect(subjectiveTrendData(daily).map((point) => point.date)).toEqual(['2026-09-01', '2026-09-03'])
  expect(daily.map((point) => point.date)).toEqual(original)
})

it('preserves missing subjective values instead of fabricating zeroes', () => {
  const sparse = [{ ...daily[0], mood: null }]
  expect(subjectiveTrendData(sparse).at(0)?.mood).toBeNull()
})

it('sorts sleep trends chronologically and converts minutes to hours', () => {
  expect(sleepTrendData(daily)).toEqual([
    { date: '2026-09-01', sleepHours: 7.25 },
    { date: '2026-09-03', sleepHours: 8 },
  ])
})
