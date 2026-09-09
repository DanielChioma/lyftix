export function formatGitHubActivityType(activityType: string) {
  const words = activityType
    .replaceAll('_', ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .toLowerCase()
  return words.length === 0 ? activityType : words[0].toUpperCase() + words.slice(1)
}

export function rankCounts<T extends { count: number }>(items: T[], label: (item: T) => string) {
  return [...items].sort((left, right) => right.count - left.count || label(left).localeCompare(label(right)))
}

export function sortDailyCounts<T extends { date: string }>(items: T[]) {
  return [...items].sort((left, right) => left.date.localeCompare(right.date))
}

export function percentage(count: number, total: number) {
  return total === 0 ? null : count / total * 100
}
