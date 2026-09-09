import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { lazy, Suspense, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getSystemMetrics, systemMetricsQueryKey } from '../api/systemMetrics'
import type { SystemMetricParameters, SystemMetricSort } from '../api/systemMetrics.types'
import { AnalyticsSection } from '../components/AnalyticsSection'
import { ApiErrorMessage } from '../components/ApiErrorMessage'
import { DateRangeSelector } from '../components/DateRangeSelector'
import { KpiCard } from '../components/KpiCard'
import { SystemMetricHistory } from '../components/SystemMetricHistory'
import type { AnalyticsDateRange } from '../api/analytics'
import { dateRangeFromSearchParams, type DateRangePreset } from '../utils/dateRange'
import { formatLoadAverage, formatMetricTimestamp, formatPercentage, percentage } from '../utils/systemMetrics'

const SystemMetricCharts = lazy(() => import('../components/SystemMetricCharts').then((module) => ({ default: module.SystemMetricCharts })))
const SORTS: SystemMetricSort[] = ['collectedAt', 'cpuPercent', 'loadAverage1m', 'hostname']
const SIZES = [10, 20]

function integerParameter(value: string | null, fallback: number) {
  const number = Number(value)
  return Number.isInteger(number) && number >= 0 ? number : fallback
}

export function SystemMetricsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { preset, range } = dateRangeFromSearchParams(searchParams)
  const hostname = searchParams.get('hostname')?.trim() ?? ''
  const [hostnameDraft, setHostnameDraft] = useState(hostname)
  const page = integerParameter(searchParams.get('page'), 0)
  const requestedSize = integerParameter(searchParams.get('size'), 10)
  const size = SIZES.includes(requestedSize) ? requestedSize : 10
  const requestedSort = searchParams.get('sort') as SystemMetricSort
  const sortBy = SORTS.includes(requestedSort) ? requestedSort : 'collectedAt'
  const historyParameters: SystemMetricParameters = { ...range, hostname, page, size, sortBy }
  const latestParameters: SystemMetricParameters = { ...range, hostname, page: 0, size: 1, sortBy: 'collectedAt' }

  const latest = useQuery({ queryKey: systemMetricsQueryKey('latest', latestParameters), queryFn: () => getSystemMetrics(latestParameters) })
  const history = useQuery({ queryKey: systemMetricsQueryKey('history', historyParameters), queryFn: () => getSystemMetrics(historyParameters), placeholderData: keepPreviousData })
  const current = latest.data?.content[0]

  function setRange(nextPreset: DateRangePreset) {
    const next = new URLSearchParams()
    if (nextPreset === 'custom') {
      next.set('startDate', range.startDate)
      next.set('endDate', range.endDate)
    } else next.set('range', nextPreset)
    if (hostname) next.set('hostname', hostname)
    setSearchParams(next)
  }
  function setCustomRange(next: AnalyticsDateRange) { setSearchParams({ startDate: next.startDate, endDate: next.endDate, ...(hostname ? { hostname } : {}) }) }
  function setHistoryParameter(name: string, value: string) {
    const next = new URLSearchParams(searchParams)
    next.set(name, value)
    if (name !== 'page') next.delete('page')
    setSearchParams(next)
  }
  function applyHostname() {
    const next = new URLSearchParams(searchParams)
    const trimmed = hostnameDraft.trim()
    if (trimmed) next.set('hostname', trimmed)
    else next.delete('hostname')
    next.delete('page')
    setSearchParams(next)
  }

  return <section aria-labelledby="system-metrics-title">
    <p className="eyebrow">Platform observability</p><h1 id="system-metrics-title">System Metrics</h1>
    <p className="lede">Inspect collected host resource usage and recent system snapshots.</p>
    <DateRangeSelector key={`${preset}-${range.startDate}-${range.endDate}`} preset={preset} range={range} onPresetChange={setRange} onCustomRangeChange={setCustomRange} />
    <section className="system-host-filter" aria-labelledby="host-filter-title"><div><h2 id="host-filter-title">Hostname filter</h2><p>Optional exact hostname; leave blank to include all hosts.</p></div><div><label><span>Hostname</span><input value={hostnameDraft} onChange={(event) => setHostnameDraft(event.target.value)} /></label><button type="button" onClick={applyHostname}>Apply host</button></div></section>

    <AnalyticsSection title="Latest collected sample" isPending={latest.isPending} error={latest.error}>
      {current ? <><p className="latest-sample-note">Newest sample in the selected range{hostname ? ` for ${hostname}` : ''}.</p><div className="kpi-grid">
        <KpiCard label="CPU usage" value={formatPercentage(current.cpuPercent)} />
        <KpiCard label="Memory usage" value={formatPercentage(percentage(current.memoryUsedBytes, current.memoryTotalBytes))} />
        <KpiCard label="Disk usage" value={formatPercentage(percentage(current.diskUsedBytes, current.diskTotalBytes))} />
        <KpiCard label="1-minute load average" value={formatLoadAverage(current.loadAverage1m)} />
        <KpiCard label="Hostname" value={current.hostname} /><KpiCard label="Source" value={current.source} />
        <KpiCard label="Last collected" value={formatMetricTimestamp(current.collectedAt)} />
      </div></> : latest.data && <p className="section-empty">No system metrics were recorded in this range.</p>}
    </AnalyticsSection>

    {history.isPending ? <div className="chart-skeleton" aria-label="Loading system metric trends" /> : history.error ? <section className="analytics-section system-trend-error"><h2>System metric trends</h2><ApiErrorMessage error={history.error} /></section> : history.data && <Suspense fallback={<div className="chart-skeleton" aria-label="Loading system metric charts" />}><SystemMetricCharts metrics={history.data.content} /></Suspense>}

    <section className="history-section" aria-labelledby="system-history-title"><div className="section-heading"><div><h2 id="system-history-title">System-metrics history</h2><p>Paged snapshots within the selected UTC date range.</p></div>{history.data && <span>{history.data.totalElements} total</span>}</div>{history.isPending ? <div className="history-skeleton" aria-label="Loading system-metrics history" /> : history.error ? <ApiErrorMessage error={history.error} /> : history.data && <SystemMetricHistory page={history.data} sortBy={sortBy} onPageChange={(next) => setHistoryParameter('page', String(next))} onSizeChange={(next) => setHistoryParameter('size', String(next))} onSortChange={(next) => setHistoryParameter('sort', next)} />}</section>
  </section>
}
