import { API_BASE_URL } from './config'
import { getGitHubActivityHistory, githubHistoryQueryKey } from './github'

const parameters = { startDate: '2026-09-01', endDate: '2026-09-30', page: 2, size: 20, sortBy: 'repositoryName' as const }

afterEach(() => vi.unstubAllGlobals())

it('sends exact half-open UTC boundaries with paging and an allowed sort', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ content: [] }), { status: 200 })))
  await getGitHubActivityHistory(parameters)
  expect(fetch).toHaveBeenCalledWith(`${API_BASE_URL}/api/github-activities/filter?start=2026-09-01T00%3A00%3A00.000Z&end=2026-10-01T00%3A00%3A00.000Z&page=2&size=20&sortBy=repositoryName`, expect.any(Object))
})

it('keys history by range and every server paging control', () => {
  expect(githubHistoryQueryKey(parameters)).toEqual(['github', 'history', '2026-09-01', '2026-09-30', 2, 20, 'repositoryName'])
})
