export function formatDuration(seconds: number) {
  if (seconds <= 0) return '0m'
  const totalMinutes = Math.round(seconds / 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (hours === 0) return `${minutes}m`
  return minutes === 0 ? `${hours}h` : `${hours}h ${minutes}m`
}
export function formatAverage(value: number | null) { return value === null ? 'No data' : value.toFixed(1) }
export function formatChartDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(year, month - 1, day))
}

export function durationBetween(startedAt: string, endedAt: string) {
  return Math.max(0, (Date.parse(endedAt) - Date.parse(startedAt)) / 1000)
}

export function formatDateTimeDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
}

export function formatTime(value: string) {
  return new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
}

export function formatNumber(value: number) {
  return new Intl.NumberFormat().format(value)
}
