export const secondsToHours = (seconds: number) => seconds / 3600
export function rankDurations<T extends { durationSeconds: number }>(items: T[], label: (item: T) => string) { return [...items].sort((a, b) => b.durationSeconds - a.durationSeconds || label(a).localeCompare(label(b))) }
export function sortCodingDaily<T extends { date: string }>(items: T[]) { return [...items].sort((a, b) => a.date.localeCompare(b.date)) }
