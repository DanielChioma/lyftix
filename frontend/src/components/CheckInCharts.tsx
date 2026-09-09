import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { CheckInAnalyticsResponse } from '../api/analytics.types'
import { formatChartDate } from '../utils/format'
import { sleepTrendData, subjectiveTrendData } from '../utils/checkIns'

export function CheckInCharts({ analytics }: { analytics: CheckInAnalyticsResponse }) {
  const subjective = subjectiveTrendData(analytics.daily)
  const sleep = sleepTrendData(analytics.daily)

  return <div className="check-in-charts">
    <section className="chart-card" aria-labelledby="subjective-trends-title">
      <div className="chart-heading">
        <h2 id="subjective-trends-title">Subjective trends</h2>
        <p>Mood, energy, focus, stress, and productivity ratings on the recorded 1–10 scale.</p>
      </div>
      {subjective.length === 0 ? <p className="empty-state">No subjective trend data is available for this range.</p> :
        <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><LineChart data={subjective} accessibilityLayer>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatChartDate} />
          <YAxis domain={[1, 10]} allowDecimals={false} label={{ value: 'Rating (1–10)', angle: -90, position: 'insideLeft' }} />
          <Tooltip labelFormatter={(value) => formatChartDate(String(value))} />
          <Legend />
          <Line connectNulls={false} dataKey="mood" name="Mood" stroke="#216e45" strokeWidth={2} />
          <Line connectNulls={false} dataKey="energy" name="Energy" stroke="#d18a24" strokeWidth={2} />
          <Line connectNulls={false} dataKey="focus" name="Focus" stroke="#315f9d" strokeWidth={2} />
          <Line connectNulls={false} dataKey="stress" name="Stress" stroke="#a34a4a" strokeWidth={2} />
          <Line connectNulls={false} dataKey="productivity" name="Productivity" stroke="#7355a3" strokeWidth={2} />
        </LineChart></ResponsiveContainer></div>}
    </section>
    <section className="chart-card" aria-labelledby="sleep-trend-title">
      <div className="chart-heading">
        <h2 id="sleep-trend-title">Sleep duration</h2>
        <p>Recorded sleep duration in hours by check-in date.</p>
      </div>
      {sleep.length === 0 ? <p className="empty-state">No sleep data is available for this range.</p> :
        <div className="chart-container"><ResponsiveContainer width="100%" height="100%"><BarChart data={sleep} accessibilityLayer>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatChartDate} />
          <YAxis unit="h" label={{ value: 'Hours', angle: -90, position: 'insideLeft' }} />
          <Tooltip labelFormatter={(value) => formatChartDate(String(value))} formatter={(value) => [`${Number(value).toFixed(1)}h`, 'Sleep']} />
          <Legend />
          <Bar dataKey="sleepHours" name="Sleep hours" fill="#487b93" />
        </BarChart></ResponsiveContainer></div>}
    </section>
  </div>
}
