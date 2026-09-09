import { apiRequest } from './client'
import type { GitHubActivityResponse, GitHubHistoryParameters, PageResponse } from './github.types'
import { startOfFollowingUtcDay, startOfUtcDay } from '../utils/dateRange'

export async function getGitHubActivityHistory(parameters: GitHubHistoryParameters) {
  const query = new URLSearchParams({
    start: startOfUtcDay(parameters.startDate),
    end: startOfFollowingUtcDay(parameters.endDate),
    page: String(parameters.page),
    size: String(parameters.size),
    sortBy: parameters.sortBy,
  })
  return (await apiRequest<PageResponse<GitHubActivityResponse>>(`/api/github-activities/filter?${query.toString()}`)).data
}

export function githubHistoryQueryKey(parameters: GitHubHistoryParameters) {
  return ['github', 'history', parameters.startDate, parameters.endDate, parameters.page, parameters.size, parameters.sortBy] as const
}
