import type { CheckInAnalyticsResponse } from '../api/analytics.types'

export type SubjectiveTrendPoint = {
  date: string
  mood: number | null
  energy: number | null
  focus: number | null
  stress: number | null
  productivity: number | null
}

export function formatSubjectiveAverage(value: number | null) {
  return value === null ? 'No data' : `${value.toFixed(1)} / 10`
}

export function formatSleepMinutes(value: number | null) {
  if (value === null) return 'No data'
  const rounded = Math.round(value)
  const hours = Math.floor(rounded / 60)
  const minutes = rounded % 60
  if (hours === 0) return `${minutes}m`
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`
}

export function subjectiveTrendData(daily: SubjectiveTrendPoint[]): SubjectiveTrendPoint[] {
  return [...daily]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map(({ date, mood, energy, focus, stress, productivity }) => ({
      date, mood, energy, focus, stress, productivity,
    }))
}

export function sleepTrendData(daily: CheckInAnalyticsResponse['daily']) {
  return [...daily]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map(({ date, sleepMinutes }) => ({ date, sleepHours: sleepMinutes / 60 }))
}
