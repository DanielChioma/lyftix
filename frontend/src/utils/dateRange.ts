import type { AnalyticsDateRange } from '../api/analytics'

export type DateRangePreset = '7d' | '30d' | '90d' | 'custom'
const PRESET_DAYS = { '7d': 7, '30d': 30, '90d': 90 } as const

export function formatLocalDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function startOfUtcDay(date: string) { return `${date}T00:00:00.000Z` }

export function startOfFollowingUtcDay(date: string) {
  const [year, month, day] = date.split('-').map(Number)
  const followingDay = new Date(Date.UTC(year, month - 1, day + 1))
  return `${followingDay.toISOString().slice(0, 10)}T00:00:00.000Z`
}

export function presetDateRange(preset: Exclude<DateRangePreset, 'custom'>, today = new Date()): AnalyticsDateRange {
  const end = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const start = new Date(end)
  start.setDate(start.getDate() - (PRESET_DAYS[preset] - 1))
  return { startDate: formatLocalDate(start), endDate: formatLocalDate(end) }
}

export function isValidDateRange(range: AnalyticsDateRange) {
  return /^\d{4}-\d{2}-\d{2}$/.test(range.startDate) && /^\d{4}-\d{2}-\d{2}$/.test(range.endDate) && range.startDate <= range.endDate
}

export function dateRangeFromSearchParams(
  params: URLSearchParams,
  today = new Date(),
): { preset: DateRangePreset; range: AnalyticsDateRange } {
  const custom = { startDate: params.get('startDate') ?? '', endDate: params.get('endDate') ?? '' }
  if (isValidDateRange(custom)) return { preset: 'custom' as const, range: custom }
  const value = params.get('range')
  const preset = value === '7d' || value === '90d' ? value : '30d'
  return { preset, range: presetDateRange(preset, today) }
}
