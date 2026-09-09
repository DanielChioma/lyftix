import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { useSearchParams } from 'react-router-dom'
import { analyticsQueryKey, getCheckInAnalytics, type AnalyticsDateRange } from '../api/analytics'
import { checkInHistoryQueryKey, getCheckInHistory } from '../api/checkIns'
import type { CheckInHistoryParameters, CheckInSort } from '../api/checkIns.types'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { CheckInHistory } from '../components/CheckInHistory'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { formatSleepMinutes, formatSubjectiveAverage } from '../utils/checkIns'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'

const CheckInCharts = lazy(() => import('../components/CheckInCharts').then((module) => ({ default: module.CheckInCharts })))
const SORTS: CheckInSort[] = ['checkInDate', 'mood', 'energy', 'focus', 'stress', 'productivity', 'sleepMinutes']
const SIZES = [10, 20]

function integerParameter(value: string | null, fallback: number) {
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 ? number : fallback
}

export function CheckInsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const page = integerParameter(searchParams.get('page'), 0)
  const requestedSize = integerParameter(searchParams.get('size'), 10)
  const size = SIZES.includes(requestedSize) ? requestedSize : 10
  const requestedSort = searchParams.get('sort') as CheckInSort
  const sortBy = SORTS.includes(requestedSort) ? requestedSort : 'checkInDate'
  const historyParameters: CheckInHistoryParameters = { ...range, page, size, sortBy }

  const analytics = useQuery({ queryKey: analyticsQueryKey('check-ins', range), queryFn: () => getCheckInAnalytics(range) })
  const history = useQuery({ queryKey: checkInHistoryQueryKey(historyParameters), queryFn: () => getCheckInHistory(historyParameters), placeholderData: keepPreviousData })

  function setRange(nextPreset: DateRangePreset) {
    if (nextPreset === 'custom') setSearchParams({ startDate: range.startDate, endDate: range.endDate })
    else setSearchParams({ range: nextPreset })
  }
  function setCustomRange(next: AnalyticsDateRange) { setSearchParams({ startDate: next.startDate, endDate: next.endDate }) }
  function setHistoryParameter(name: string, value: string) {
    const next = new URLSearchParams(searchParams)
    next.set(name, value)
    if (name !== 'page') next.delete('page')
    setSearchParams(next)
  }

  const checkInCount = analytics.data?.daily.length ?? 0
  return <section aria-labelledby="check-ins-title">
    <p className="eyebrow">Daily wellbeing</p>
    <h1 id="check-ins-title">Daily Check-ins</h1>
    <p className="lede">Review recorded wellbeing ratings, sleep duration, and check-in history.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={setRange} onCustomRangeChange={setCustomRange} />

    <AnalyticsSection title="Check-in summary" isPending={analytics.isPending} error={analytics.error}>
      <div className="kpi-grid">
        <KpiCard label="Average mood (1–10)" value={formatSubjectiveAverage(analytics.data?.averageMood ?? null)} />
        <KpiCard label="Average energy (1–10)" value={formatSubjectiveAverage(analytics.data?.averageEnergy ?? null)} />
        <KpiCard label="Average focus (1–10)" value={formatSubjectiveAverage(analytics.data?.averageFocus ?? null)} />
        <KpiCard label="Average stress (1–10)" value={formatSubjectiveAverage(analytics.data?.averageStress ?? null)} />
        <KpiCard label="Average productivity (1–10)" value={formatSubjectiveAverage(analytics.data?.averageProductivity ?? null)} />
        <KpiCard label="Average sleep" value={formatSleepMinutes(analytics.data?.averageSleepMinutes ?? null)} />
      </div>
      {analytics.data && <div className="check-in-period-summary" aria-label="Period summary">
        <strong>{checkInCount} check-in{checkInCount === 1 ? '' : 's'}</strong>
        <span>{analytics.data.startDate} to {analytics.data.endDate}</span>
        <span>Average sleep: {formatSleepMinutes(analytics.data.averageSleepMinutes)}</span>
      </div>}
      {checkInCount === 0 && analytics.data && <p className="section-empty">No daily check-ins were recorded in this range.</p>}
    </AnalyticsSection>

    {!analytics.isPending && !analytics.error && analytics.data && <Suspense fallback={<div className="chart-skeleton" aria-label="Loading check-in charts" />}><CheckInCharts analytics={analytics.data} /></Suspense>}

    <section className="history-section" aria-labelledby="check-in-history-title">
      <div className="section-heading"><div><h2 id="check-in-history-title">Check-in history</h2><p>Paged check-ins within the selected inclusive date range.</p></div>{history.data && <span>{history.data.totalElements} total</span>}</div>
      {history.isPending ? <div className="history-skeleton" aria-label="Loading check-in history" /> : history.error ? <ApiErrorMessage error={history.error} /> : history.data && <CheckInHistory page={history.data} sortBy={sortBy} onPageChange={(next) => setHistoryParameter('page', String(next))} onSizeChange={(next) => setHistoryParameter('size', String(next))} onSortChange={(next) => setHistoryParameter('sort', next)} />}
    </section>
  </section>
}
