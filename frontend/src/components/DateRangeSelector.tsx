import { useState } from 'react'
import type { AnalyticsDateRange } from '../api/analytics'
import { isValidDateRange, type DateRangePreset } from '../utils/dateRange'

export function DateRangeSelector({ preset, range, onPresetChange, onCustomRangeChange }: {
  preset: DateRangePreset; range: AnalyticsDateRange
  onPresetChange: (preset: DateRangePreset) => void
  onCustomRangeChange: (range: AnalyticsDateRange) => void
}) {
  const [draft, setDraft] = useState(range)
  const [error, setError] = useState<string | null>(null)
  function apply() {
    if (!isValidDateRange(draft)) { setError('Start date must be on or before end date.'); return }
    setError(null); onCustomRangeChange(draft)
  }

  return <section className="date-range" aria-labelledby="date-range-title">
    <div><h2 id="date-range-title">Date range</h2><p>{range.startDate} to {range.endDate}</p></div>
    <div className="range-controls">
      <label><span>Preset</span><select value={preset} onChange={(event) => {
        const value = event.target.value as DateRangePreset
        onPresetChange(value)
      }}><option value="7d">Last 7 days</option><option value="30d">Last 30 days</option><option value="90d">Last 90 days</option><option value="custom">Custom</option></select></label>
      {preset === 'custom' && <div className="custom-range">
        <label><span>Start date</span><input type="date" value={draft.startDate} onChange={(event) => setDraft({ ...draft, startDate: event.target.value })} /></label>
        <label><span>End date</span><input type="date" value={draft.endDate} onChange={(event) => setDraft({ ...draft, endDate: event.target.value })} /></label>
        <button type="button" onClick={apply}>Apply</button>
      </div>}
    </div>
    {error && <p className="field-error" role="alert">{error}</p>}
  </section>
}
