import { useQuery } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { useSearchParams } from 'react-router-dom'
import { analyticsQueryKey, getCheckInAnalytics, getDailyAnalyticsSummary, type AnalyticsDateRange } from '../api/analytics'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { ProductivityComparison } from '../components/ProductivityComparison'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatAverage, formatDuration } from '../utils/format'
import { totalActivity } from '../utils/productivityAnalysis'

const ProductivityCharts = lazy(() => import('../components/ProductivityCharts').then((module) => ({ default: module.ProductivityCharts })))

export function ProductivityPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const daily = useQuery({ queryKey: analyticsQueryKey('daily-summary', range), queryFn: () => getDailyAnalyticsSummary(range) })
  const checkIns = useQuery({ queryKey: analyticsQueryKey('check-ins', range), queryFn: () => getCheckInAnalytics(range) })
  const totals = totalActivity(daily.data?.daily ?? [])
  const selectCustom = (next: AnalyticsDateRange) => setSearchParams({ startDate: next.startDate, endDate: next.endDate })
  const selectPreset = (next: DateRangePreset) => next === 'custom' ? selectCustom(range) : setSearchParams({ range: next })

  return <section aria-labelledby="productivity-title">
    <p className="eyebrow">Cross-domain patterns</p><h1 id="productivity-title">Productivity</h1>
    <p className="lede">Compare recorded daily signals with coding, GitHub, and workout activity. Patterns are descriptive and do not establish causation.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={selectPreset} onCustomRangeChange={selectCustom} />

    <div className="analytics-sections productivity-kpis">
      <AnalyticsSection title="Daily signals" isPending={checkIns.isPending} error={checkIns.error}><div className="kpi-grid"><KpiCard label="Average productivity" value={formatAverage(checkIns.data?.averageProductivity ?? null)} /><KpiCard label="Average focus" value={formatAverage(checkIns.data?.averageFocus ?? null)} /><KpiCard label="Average energy" value={formatAverage(checkIns.data?.averageEnergy ?? null)} /></div>{checkIns.data?.daily.length === 0 && <p className="section-empty">No check-ins recorded in this range.</p>}</AnalyticsSection>
      <AnalyticsSection title="Recorded activity" isPending={daily.isPending} error={daily.error}><div className="kpi-grid"><KpiCard label="Coding time" value={formatDuration(totals.codingDurationSeconds)} /><KpiCard label="Coding sessions" value={totals.codingSessionCount} /><KpiCard label="GitHub activities" value={totals.githubActivityCount} /><KpiCard label="Workout count" value={totals.workoutCount} /><KpiCard label="Workout duration" value={formatDuration(totals.workoutDurationSeconds)} /></div>{daily.data?.daily.length === 0 && <p className="section-empty">No daily activity recorded in this range.</p>}</AnalyticsSection>
    </div>

    {daily.isPending ? <div className="chart-skeleton" aria-label="Loading productivity patterns" /> : daily.error ? <section className="analytics-section patterns-error"><h2>Cross-domain patterns</h2><ApiErrorMessage error={daily.error} /></section> : <><Suspense fallback={<div className="chart-skeleton" aria-label="Loading productivity charts" />}><ProductivityCharts daily={daily.data?.daily ?? []} /></Suspense><ProductivityComparison daily={daily.data?.daily ?? []} /></>}
  </section>
}
