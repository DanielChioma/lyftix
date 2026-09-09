import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { analyticsQueryKey, getCodingAnalytics, type AnalyticsDateRange } from '../api/analytics'
import { codingHistoryQueryKey, getCodingHistory } from '../api/coding'
import type { CodingHistoryParameters, CodingSort } from '../api/coding.types'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { CodingCharts } from '../components/CodingCharts'
import { CodingHistory } from '../components/CodingHistory'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { rankDurations } from '../utils/coding'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatDuration } from '../utils/format'

const SORTS: CodingSort[] = ['startedAt', 'projectName', 'language']
const SIZES = [10, 20]
function integerParameter(value: string | null, fallback: number) { const number = Number(value); return Number.isInteger(number) && number >= 0 ? number : fallback }

export function CodingPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const page = integerParameter(searchParams.get('page'), 0)
  const requestedSize = integerParameter(searchParams.get('size'), 10)
  const size = SIZES.includes(requestedSize) ? requestedSize : 10
  const requestedSort = searchParams.get('sort') as CodingSort
  const sortBy = SORTS.includes(requestedSort) ? requestedSort : 'startedAt'
  const historyParameters: CodingHistoryParameters = { ...range, page, size, sortBy }
  const analytics = useQuery({ queryKey: analyticsQueryKey('coding', range), queryFn: () => getCodingAnalytics(range) })
  const history = useQuery({ queryKey: codingHistoryQueryKey(historyParameters), queryFn: () => getCodingHistory(historyParameters), placeholderData: keepPreviousData })
  const projects = rankDurations(analytics.data?.durationsByProject ?? [], (item) => item.project)
  const languages = rankDurations(analytics.data?.durationsByLanguage ?? [], (item) => item.language)
  function setRange(next: DateRangePreset) { if (next === 'custom') setSearchParams({ startDate: range.startDate, endDate: range.endDate }); else setSearchParams({ range: next }) }
  function setCustomRange(next: AnalyticsDateRange) { setSearchParams({ startDate: next.startDate, endDate: next.endDate }) }
  function setHistoryParameter(name: string, value: string) { const next = new URLSearchParams(searchParams); next.set(name, value); if (name !== 'page') next.delete('page'); setSearchParams(next) }
  return <section aria-labelledby="coding-title">
    <p className="eyebrow">Development focus</p><h1 id="coding-title">Coding Sessions</h1><p className="lede">Explore coding time, projects, languages, and recent sessions.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={setRange} onCustomRangeChange={setCustomRange} />
    <AnalyticsSection title="Coding summary" isPending={analytics.isPending} error={analytics.error}><div className="kpi-grid"><KpiCard label="Total sessions" value={analytics.data?.totalSessions ?? 0} /><KpiCard label="Total duration" value={formatDuration(analytics.data?.totalDurationSeconds ?? 0)} /><KpiCard label="Average session" value={analytics.data?.averageDurationSeconds == null ? 'No data' : formatDuration(analytics.data.averageDurationSeconds)} /><KpiCard label="Most active project" value={projects[0]?.project ?? 'No data'} /><KpiCard label="Primary language" value={languages[0]?.language ?? 'No data'} /></div>{analytics.data?.totalSessions === 0 && <p className="section-empty">No coding sessions were recorded in this range.</p>}</AnalyticsSection>
    {!analytics.isPending && !analytics.error && analytics.data && <><CodingCharts analytics={analytics.data} /><div className="analytics-sections coding-breakdowns"><section className="analytics-section"><h2>Projects</h2>{projects.length ? projects.map((item) => <p className="breakdown-row" key={item.project}><span>{item.project}</span><strong>{formatDuration(item.durationSeconds)}</strong></p>) : <p className="section-empty">No project activity is available for this range.</p>}</section><section className="analytics-section"><h2>Languages</h2>{languages.length ? languages.map((item) => <p className="breakdown-row" key={item.language}><span>{item.language}</span><strong>{formatDuration(item.durationSeconds)}</strong></p>) : <p className="section-empty">No language activity is available for this range.</p>}</section></div></>}
    <section className="history-section" aria-labelledby="coding-history-title"><div className="section-heading"><div><h2 id="coding-history-title">Coding-session history</h2><p>Paged sessions within the selected date range.</p></div>{history.data && <span>{history.data.totalElements} total</span>}</div>{history.isPending ? <div className="history-skeleton" aria-label="Loading coding-session history" /> : history.error ? <ApiErrorMessage error={history.error} /> : history.data && <CodingHistory page={history.data} sortBy={sortBy} onPageChange={(next) => setHistoryParameter('page', String(next))} onSizeChange={(next) => setHistoryParameter('size', String(next))} onSortChange={(next) => setHistoryParameter('sort', next)} />}</section>
  </section>
}
