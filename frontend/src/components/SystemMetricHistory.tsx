import type { SystemMetricPage, SystemMetricResponse, SystemMetricSort } from '../api/systemMetrics.types'
import { formatLoadAverage, formatMetricTimestamp, formatPercentage, percentage } from '../utils/systemMetrics'

function MetricValues({ metric }: { metric: SystemMetricResponse }) {
  return <><strong data-label="Collected">{formatMetricTimestamp(metric.collectedAt)}</strong><span data-label="Hostname">{metric.hostname}</span><span data-label="Source">{metric.source}</span><span data-label="CPU">{formatPercentage(metric.cpuPercent)}</span><span data-label="Memory">{formatPercentage(percentage(metric.memoryUsedBytes, metric.memoryTotalBytes))}</span><span data-label="Disk">{formatPercentage(percentage(metric.diskUsedBytes, metric.diskTotalBytes))}</span><span data-label="1m load">{formatLoadAverage(metric.loadAverage1m)}</span></>
}

export function SystemMetricHistory({ page, sortBy, onPageChange, onSizeChange, onSortChange }: {
  page: SystemMetricPage
  sortBy: SystemMetricSort
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
  onSortChange: (sort: SystemMetricSort) => void
}) {
  return <>
    <div className="history-controls">
      <label><span>Sort metrics</span><select value={sortBy} onChange={(event) => onSortChange(event.target.value as SystemMetricSort)}><option value="collectedAt">Newest collected</option><option value="cpuPercent">Highest CPU</option><option value="loadAverage1m">Highest load</option><option value="hostname">Hostname Z–A</option></select></label>
      <label><span>Page size</span><select value={page.size} onChange={(event) => onSizeChange(Number(event.target.value))}><option value="10">10</option><option value="20">20</option></select></label>
    </div>
    {page.empty ? <p className="empty-state">No system metrics were recorded in this range.</p> : <>
      <div className="system-table-wrap"><table className="system-table"><thead><tr><th scope="col">Collected</th><th scope="col">Hostname</th><th scope="col">Source</th><th scope="col">CPU</th><th scope="col">Memory</th><th scope="col">Disk</th><th scope="col">1m load</th></tr></thead><tbody>{page.content.map((metric) => <tr key={metric.id}><td>{formatMetricTimestamp(metric.collectedAt)}</td><td>{metric.hostname}</td><td>{metric.source}</td><td>{formatPercentage(metric.cpuPercent)}</td><td>{formatPercentage(percentage(metric.memoryUsedBytes, metric.memoryTotalBytes))}</td><td>{formatPercentage(percentage(metric.diskUsedBytes, metric.diskTotalBytes))}</td><td>{formatLoadAverage(metric.loadAverage1m)}</td></tr>)}</tbody></table></div>
      <div className="system-cards">{page.content.map((metric) => <article key={metric.id} aria-label={`${metric.hostname} system metric`}><MetricValues metric={metric} /></article>)}</div>
    </>}
    <nav className="pagination" aria-label="System metrics history pagination"><button type="button" disabled={page.first} onClick={() => onPageChange(page.number - 1)} aria-label="Previous system-metrics page">Previous</button><span>Page {page.totalPages ? page.number + 1 : 0} of {page.totalPages} · {page.totalElements} total</span><button type="button" disabled={page.last || !page.totalPages} onClick={() => onPageChange(page.number + 1)} aria-label="Next system-metrics page">Next</button></nav>
  </>
}
