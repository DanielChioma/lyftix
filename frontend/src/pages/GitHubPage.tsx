import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { useSearchParams } from 'react-router-dom'
import { analyticsQueryKey, getGitHubAnalytics, type AnalyticsDateRange } from '../api/analytics'
import { getGitHubActivityHistory, githubHistoryQueryKey } from '../api/github'
import type { GitHubHistoryParameters, GitHubSort } from '../api/github.types'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { GitHubHistory } from '../components/GitHubHistory'
import { KpiCard } from '../components/KpiCard'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatNumber } from '../utils/format'
import { formatGitHubActivityType, rankCounts } from '../utils/github'

const GitHubCharts = lazy(() => import('../components/GitHubCharts').then((module) => ({ default: module.GitHubCharts })))
const SORTS: GitHubSort[] = ['occurredAt', 'activityType', 'repositoryName']
const SIZES = [10, 20]

function integerParameter(value: string | null, fallback: number) { const number = Number(value); return Number.isInteger(number) && number >= 0 ? number : fallback }

export function GitHubPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const page = integerParameter(searchParams.get('page'), 0)
  const requestedSize = integerParameter(searchParams.get('size'), 10)
  const size = SIZES.includes(requestedSize) ? requestedSize : 10
  const requestedSort = searchParams.get('sort') as GitHubSort
  const sortBy = SORTS.includes(requestedSort) ? requestedSort : 'occurredAt'
  const historyParameters: GitHubHistoryParameters = { ...range, page, size, sortBy }
  const analytics = useQuery({ queryKey: analyticsQueryKey('github', range), queryFn: () => getGitHubAnalytics(range) })
  const history = useQuery({ queryKey: githubHistoryQueryKey(historyParameters), queryFn: () => getGitHubActivityHistory(historyParameters), placeholderData: keepPreviousData })
  const repositories = rankCounts(analytics.data?.countsByRepository ?? [], (item) => item.repository)
  const activityTypes = rankCounts(analytics.data?.countsByActivityType ?? [], (item) => item.activityType)

  function setRange(next: DateRangePreset) { if (next === 'custom') setSearchParams({ startDate: range.startDate, endDate: range.endDate }); else setSearchParams({ range: next }) }
  function setCustomRange(next: AnalyticsDateRange) { setSearchParams({ startDate: next.startDate, endDate: next.endDate }) }
  function setHistoryParameter(name: string, value: string) { const next = new URLSearchParams(searchParams); next.set(name, value); if (name !== 'page') next.delete('page'); setSearchParams(next) }

  return <section aria-labelledby="github-page-title"><p className="eyebrow">Development activity</p><h1 id="github-page-title">GitHub Activity</h1><p className="lede">Explore recorded GitHub events, repository momentum, and recent activity.</p><DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={setRange} onCustomRangeChange={setCustomRange} />
    <AnalyticsSection title="GitHub summary" isPending={analytics.isPending} error={analytics.error}><div className="kpi-grid"><KpiCard label="Total activities" value={formatNumber(analytics.data?.totalActivities ?? 0)} /><KpiCard label="Active repositories" value={formatNumber(analytics.data?.countsByRepository.length ?? 0)} /><KpiCard label="Most active repository" value={repositories[0]?.repository ?? 'No data'} /><KpiCard label="Most common activity" value={activityTypes[0] ? formatGitHubActivityType(activityTypes[0].activityType) : 'No data'} /></div>{analytics.data?.totalActivities === 0 && <p className="section-empty">No GitHub activity was recorded in this range.</p>}</AnalyticsSection>
    {!analytics.isPending && !analytics.error && analytics.data && <Suspense fallback={<div className="chart-skeleton" aria-label="Loading GitHub charts" />}><GitHubCharts analytics={analytics.data} /></Suspense>}
    <section className="history-section" aria-labelledby="github-history-title"><div className="section-heading"><div><h2 id="github-history-title">Activity history</h2><p>Paged GitHub activity within the selected date range.</p></div>{history.data && <span>{formatNumber(history.data.totalElements)} total</span>}</div>{history.isPending ? <div className="history-skeleton" aria-label="Loading GitHub activity history" /> : history.error ? <ApiErrorMessage error={history.error} /> : history.data && <GitHubHistory page={history.data} sortBy={sortBy} onPageChange={(next) => setHistoryParameter('page', String(next))} onSizeChange={(next) => setHistoryParameter('size', String(next))} onSortChange={(next) => setHistoryParameter('sort', next)} />}</section>
  </section>
}
