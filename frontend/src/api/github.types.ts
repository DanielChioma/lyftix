export interface GitHubActivityResponse {
  id: number
  activityType: string
  repositoryName: string
  repositoryOwner: string
  occurredAt: string
  externalId: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
}

export type GitHubSort = 'occurredAt' | 'activityType' | 'repositoryName'

export interface GitHubHistoryParameters {
  startDate: string
  endDate: string
  page: number
  size: number
  sortBy: GitHubSort
}
