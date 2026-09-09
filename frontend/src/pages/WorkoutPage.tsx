import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { useSearchParams } from 'react-router-dom'
import { analyticsQueryKey, getWorkoutAnalytics, type AnalyticsDateRange } from '../api/analytics'
import { getWorkoutHistory, workoutHistoryQueryKey } from '../api/workouts'
import type { WorkoutHistoryParameters, WorkoutSort } from '../api/workouts.types'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { WorkoutHistory } from '../components/WorkoutHistory'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatAverage, formatDuration, formatNumber } from '../utils/format'

const WorkoutCharts = lazy(() => import('../components/WorkoutCharts').then((module) => ({ default: module.WorkoutCharts })))
const SORTS: WorkoutSort[] = ['startedAt', 'caloriesBurned', 'intensity']
const SIZES = [10, 20]

function integerParameter(value: string | null, fallback: number) {
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 ? number : fallback
}

export function WorkoutPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const page = integerParameter(searchParams.get('page'), 0)
  const requestedSize = integerParameter(searchParams.get('size'), 10)
  const size = SIZES.includes(requestedSize) ? requestedSize : 10
  const requestedSort = searchParams.get('sort') as WorkoutSort
  const sortBy = SORTS.includes(requestedSort) ? requestedSort : 'startedAt'
  const historyParameters: WorkoutHistoryParameters = { ...range, page, size, sortBy }

  const analytics = useQuery({ queryKey: analyticsQueryKey('workouts', range), queryFn: () => getWorkoutAnalytics(range) })
  const history = useQuery({ queryKey: workoutHistoryQueryKey(historyParameters), queryFn: () => getWorkoutHistory(historyParameters), placeholderData: keepPreviousData })

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

  return <section aria-labelledby="workout-page-title">
    <p className="eyebrow">Training analytics</p><h1 id="workout-page-title">Workout Analytics</h1>
    <p className="lede">Explore workout volume, effort, calories, and recent history.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={setRange} onCustomRangeChange={setCustomRange} />

    <AnalyticsSection title="Workout summary" isPending={analytics.isPending} error={analytics.error}>
      <div className="kpi-grid">
        <KpiCard label="Total workouts" value={analytics.data?.totalWorkouts ?? 0} />
        <KpiCard label="Total duration" value={formatDuration(analytics.data?.totalDurationSeconds ?? 0)} />
        <KpiCard label="Calories burned" value={formatNumber(analytics.data?.totalCaloriesBurned ?? 0)} />
        <KpiCard label="Average intensity" value={formatAverage(analytics.data?.averageIntensity ?? null)} />
      </div>
      {analytics.data?.totalWorkouts === 0 && <p className="section-empty">No workouts recorded.</p>}
    </AnalyticsSection>

    {!analytics.isPending && !analytics.error && analytics.data && <Suspense fallback={<div className="chart-skeleton" aria-label="Loading workout charts" />}><WorkoutCharts analytics={analytics.data} /></Suspense>}

    <section className="history-section" aria-labelledby="workout-history-title">
      <div className="section-heading"><div><h2 id="workout-history-title">Workout history</h2><p>Paged workouts within the selected date range.</p></div>{history.data && <span>{formatNumber(history.data.totalElements)} total</span>}</div>
      {history.isPending ? <div className="history-skeleton" aria-label="Loading workout history" /> : history.error ? <ApiErrorMessage error={history.error} /> : history.data && <WorkoutHistory page={history.data} sortBy={sortBy} onPageChange={(next) => setHistoryParameter('page', String(next))} onSizeChange={(next) => setHistoryParameter('size', String(next))} onSortChange={(next) => setHistoryParameter('sort', next)} />}
    </section>
  </section>
}
