export function toLocalDateTimeValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return offsetDate.toISOString().slice(0, 16)
}

export function localDateTimeToInstant(value: string) {
  return new Date(value).toISOString()
}
