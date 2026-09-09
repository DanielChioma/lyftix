import type { DailyAnalyticsSummary } from '../api/analytics.types'
import { analyzeProductivity, describeProductivityPattern } from '../utils/productivityAnalysis'

const value = (number: number | null, suffix = '') => number === null ? 'No data' : `${number.toFixed(1)}${suffix}`

export function ProductivityComparison({ daily }: { daily: DailyAnalyticsSummary[] }) {
  const analysis = analyzeProductivity(daily)
  const description = describeProductivityPattern(analysis)

  return <section className="comparison-card" aria-labelledby="comparison-title">
    <div className="section-heading"><div><h2 id="comparison-title">Higher vs lower productivity days</h2><p>Days above the median score compared with days at or below it.</p></div><span>{analysis.recordedDayCount} recorded {analysis.recordedDayCount === 1 ? 'day' : 'days'}</span></div>
    {!analysis.hasSufficientSample || !analysis.higher || !analysis.lower ? <p className="section-empty">At least two recorded days in each group are needed for this comparison.</p> : <>
      <div className="comparison-grid">
        <div className="comparison-label">Measure</div><div className="comparison-label">Higher ({analysis.higher.dayCount})</div><div className="comparison-label">At/below median ({analysis.lower.dayCount})</div>
        <span>Coding time</span><strong>{value(analysis.higher.averageCodingHours, 'h')}</strong><strong>{value(analysis.lower.averageCodingHours, 'h')}</strong>
        <span>Workout time</span><strong>{value(analysis.higher.averageWorkoutHours, 'h')}</strong><strong>{value(analysis.lower.averageWorkoutHours, 'h')}</strong>
        <span>GitHub activities</span><strong>{value(analysis.higher.averageGitHubActivities)}</strong><strong>{value(analysis.lower.averageGitHubActivities)}</strong>
        <span>Energy</span><strong>{value(analysis.higher.averageEnergy)}</strong><strong>{value(analysis.lower.averageEnergy)}</strong>
        <span>Focus</span><strong>{value(analysis.higher.averageFocus)}</strong><strong>{value(analysis.lower.averageFocus)}</strong>
      </div>
      <p className="pattern-summary">{description}</p>
    </>}
    {analysis.medianProductivity !== null && <p className="method-note">Median productivity: {analysis.medianProductivity.toFixed(1)}. Only days with a recorded productivity score are grouped; this comparison describes association and does not imply causation.</p>}
  </section>
}
