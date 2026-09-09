import { lazy, Suspense } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { AnalyticsDateRange } from '../api/analytics'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { useDashboardAnalytics } from '../hooks/useDashboardAnalytics'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatAverage, formatDuration } from '../utils/format'

const DashboardCharts = lazy(() => import('../components/DashboardCharts').then((module) => ({
  default: module.DashboardCharts,
})))

export function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const analytics = useDashboardAnalytics(range)
  const selectCustom = (next: AnalyticsDateRange) => setSearchParams({ startDate: next.startDate, endDate: next.endDate })
  const selectPreset = (next: DateRangePreset) => next === 'custom' ? selectCustom(range) : setSearchParams({ range: next })

  return <section aria-labelledby="dashboard-title">
    <p className="eyebrow">Personal analytics</p><h1 id="dashboard-title">Lyftix Overview</h1>
    <p className="lede">A shared view of your training, development activity, and daily signals.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={selectPreset} onCustomRangeChange={selectCustom} />

    <div className="analytics-sections">
      <AnalyticsSection title="Workouts" isPending={analytics.workouts.isPending} error={analytics.workouts.error}>
        <div className="kpi-grid"><KpiCard label="Workout count" value={analytics.workouts.data?.totalWorkouts ?? 0} /><KpiCard label="Workout duration" value={formatDuration(analytics.workouts.data?.totalDurationSeconds ?? 0)} /><KpiCard label="Calories burned" value={analytics.workouts.data?.totalCaloriesBurned ?? 0} /></div>
        {analytics.workouts.data?.totalWorkouts === 0 && <p className="section-empty">No workouts recorded.</p>}
      </AnalyticsSection>
      <AnalyticsSection title="Coding" isPending={analytics.coding.isPending} error={analytics.coding.error}>
        <div className="kpi-grid"><KpiCard label="Coding sessions" value={analytics.coding.data?.totalSessions ?? 0} /><KpiCard label="Coding duration" value={formatDuration(analytics.coding.data?.totalDurationSeconds ?? 0)} /></div>
        {analytics.coding.data?.totalSessions === 0 && <p className="section-empty">No coding sessions recorded.</p>}
      </AnalyticsSection>
      <AnalyticsSection title="GitHub" isPending={analytics.github.isPending} error={analytics.github.error}>
        <div className="kpi-grid"><KpiCard label="GitHub activity" value={analytics.github.data?.totalActivities ?? 0} /></div>
        {analytics.github.data?.totalActivities === 0 && <p className="section-empty">No GitHub activity recorded.</p>}
      </AnalyticsSection>
      <AnalyticsSection title="Daily check-ins" isPending={analytics.checkIns.isPending} error={analytics.checkIns.error}>
        <div className="kpi-grid"><KpiCard label="Average energy" value={formatAverage(analytics.checkIns.data?.averageEnergy ?? null)} /><KpiCard label="Average focus" value={formatAverage(analytics.checkIns.data?.averageFocus ?? null)} /><KpiCard label="Average productivity" value={formatAverage(analytics.checkIns.data?.averageProductivity ?? null)} /></div>
        {analytics.checkIns.data?.daily.length === 0 && <p className="section-empty">No check-ins recorded.</p>}
      </AnalyticsSection>
    </div>

    {analytics.daily.isPending ? <div className="chart-skeleton" aria-label="Loading daily trends" /> : analytics.daily.error ? <section className="analytics-section"><h2>Daily trends</h2><ApiErrorMessage error={analytics.daily.error} /></section> : <Suspense fallback={<div className="chart-skeleton" aria-label="Loading charts" />}><DashboardCharts daily={analytics.daily.data?.daily ?? []} /></Suspense>}
  </section>
}
