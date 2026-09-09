import { formatGitHubActivityType, percentage, rankCounts, sortDailyCounts } from './github'

it('formats underscore and camel-case activity types without an event allowlist', () => {
  expect(formatGitHubActivityType('PULL_REQUEST_MERGED')).toBe('Pull request merged')
  expect(formatGitHubActivityType('PullRequestOpened')).toBe('Pull request opened')
  expect(formatGitHubActivityType('FutureEventKind')).toBe('Future event kind')
})

it('ranks counts deterministically and does not mutate the input', () => {
  const source = [{ repository: 'z/repo', count: 2 }, { repository: 'a/repo', count: 2 }, { repository: 'b/repo', count: 3 }]
  expect(rankCounts(source, (item) => item.repository).map((item) => item.repository)).toEqual(['b/repo', 'a/repo', 'z/repo'])
  expect(source[0].repository).toBe('z/repo')
})

it('sorts daily data chronologically and handles percentages without dividing by zero', () => {
  expect(sortDailyCounts([{ date: '2026-09-02' }, { date: '2026-09-01' }]).map((item) => item.date)).toEqual(['2026-09-01', '2026-09-02'])
  expect(percentage(2, 8)).toBe(25)
  expect(percentage(0, 0)).toBeNull()
})
