import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { SystemMetricResponse } from '../api/systemMetrics.types'
import { metricTrendData } from '../utils/systemMetrics'

function timeLabel(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }).format(new Date(value))
}

export function SystemMetricCharts({ metrics }: { metrics: SystemMetricResponse[] }) {
  const data = metricTrendData(metrics)
  return <div className="system-metric-charts">
    <section className="chart-card" aria-labelledby="resource-usage-title">
      <div className="chart-heading"><h2 id="resource-usage-title">Resource usage</h2><p>CPU, memory, and disk usage percentages for samples on the loaded history page.</p></div>
      {data.length === 0 ? <p className="empty-state">No system metrics were recorded in this range.</p> : <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={data} accessibilityLayer>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="collectedAt" tickFormatter={timeLabel} /><YAxis domain={[0, 100]} unit="%" />
        <Tooltip labelFormatter={(value) => timeLabel(String(value))} formatter={(value) => [`${Number(value).toFixed(1)}%`]} /><Legend />
        <Line connectNulls={false} dataKey="cpuPercent" name="CPU" stroke="var(--chart-green)" strokeWidth={2} />
        <Line connectNulls={false} dataKey="memoryPercent" name="Memory" stroke="var(--chart-blue)" strokeWidth={2} />
        <Line connectNulls={false} dataKey="diskPercent" name="Disk" stroke="var(--chart-orange)" strokeWidth={2} />
      </LineChart></ResponsiveContainer></div>}
    </section>
    <section className="chart-card" aria-labelledby="load-average-title">
      <div className="chart-heading"><h2 id="load-average-title">One-minute load average</h2><p>Recorded 1-minute system load for samples on the loaded history page; this is not a percentage.</p></div>
      {data.length === 0 || data.every((metric) => metric.loadAverage1m === null) ? <p className="empty-state">No load-average samples are available for this range.</p> : <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={data} accessibilityLayer>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="collectedAt" tickFormatter={timeLabel} /><YAxis />
        <Tooltip labelFormatter={(value) => timeLabel(String(value))} /><Legend />
        <Line connectNulls={false} dataKey="loadAverage1m" name="1-minute load average" stroke="var(--chart-purple)" strokeWidth={2} />
      </LineChart></ResponsiveContainer></div>}
    </section>
  </div>
}
