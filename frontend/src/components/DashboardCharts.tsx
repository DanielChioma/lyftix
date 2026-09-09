import type { ReactNode } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DailyAnalyticsSummary } from '../api/analytics.types'
import { transformDailyChartData } from '../utils/chartData'
import { formatChartDate } from '../utils/format'

function ChartFrame({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  const id = `${title.toLowerCase().replaceAll(' ', '-')}-chart-title`
  return <section className="chart-card" aria-labelledby={id}>
    <div className="chart-heading"><h2 id={id}>{title}</h2><p>{description}</p></div>
    {children}
  </section>
}

export function DashboardCharts({ daily }: { daily: DailyAnalyticsSummary[] }) {
  const data = transformDailyChartData(daily)
  const hasActivity = data.some((day) => day.workoutCount + day.codingSessionCount + day.githubActivityCount > 0)
  const hasSubjective = data.some((day) => day.energy !== null || day.focus !== null || day.productivity !== null)
  const hasDuration = data.some((day) => day.codingDurationSeconds + day.workoutDurationSeconds > 0)

  return <div className="charts-grid">
    <ChartFrame title="Daily activity" description="Event counts by day; series use distinct labels and patterns.">
      {!hasActivity ? <p className="empty-state">No workout, coding, or GitHub activity recorded in this range.</p> : <div className="chart-container">
        <ResponsiveContainer width="100%" height="100%"><BarChart data={data} accessibilityLayer>
          <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis allowDecimals={false} />
          <Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend />
          <Bar dataKey="workoutCount" name="Workouts" fill="var(--chart-green)" /><Bar dataKey="codingSessionCount" name="Coding sessions" fill="var(--chart-blue)" /><Bar dataKey="githubActivityCount" name="GitHub activity" fill="var(--chart-orange)" />
        </BarChart></ResponsiveContainer>
      </div>}
    </ChartFrame>
    <ChartFrame title="Subjective trends" description="Daily energy, focus, and productivity ratings.">
      {!hasSubjective ? <p className="empty-state">No check-ins recorded in this range.</p> : <div className="chart-container">
        <ResponsiveContainer width="100%" height="100%"><LineChart data={data} accessibilityLayer>
          <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis domain={[0, 10]} />
          <Tooltip labelFormatter={(value) => formatChartDate(String(value))} /><Legend />
          <Line connectNulls={false} dataKey="energy" name="Energy" stroke="var(--chart-green)" strokeWidth={2} />
          <Line connectNulls={false} dataKey="focus" name="Focus" stroke="var(--chart-blue)" strokeDasharray="6 3" strokeWidth={2} />
          <Line connectNulls={false} dataKey="productivity" name="Productivity" stroke="var(--chart-orange)" strokeDasharray="2 3" strokeWidth={2} />
        </LineChart></ResponsiveContainer>
      </div>}
    </ChartFrame>
    <ChartFrame title="Time invested" description="Daily coding and workout duration in hours.">
      {!hasDuration ? <p className="empty-state">No coding or workout time recorded in this range.</p> : <div className="chart-container">
        <ResponsiveContainer width="100%" height="100%"><LineChart data={data} accessibilityLayer>
          <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tickFormatter={formatChartDate} /><YAxis unit="h" />
          <Tooltip labelFormatter={(value) => formatChartDate(String(value))} formatter={(value) => [`${value}h`]} /><Legend />
          <Line dataKey="codingHours" name="Coding hours" stroke="var(--chart-blue)" strokeWidth={2} />
          <Line dataKey="workoutHours" name="Workout hours" stroke="var(--chart-green)" strokeDasharray="6 3" strokeWidth={2} />
        </LineChart></ResponsiveContainer>
      </div>}
    </ChartFrame>
  </div>
}
