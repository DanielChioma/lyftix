import{rankDurations,secondsToHours,sortCodingDaily}from'./coding'
it('converts coding seconds to hours',()=>expect(secondsToHours(5400)).toBe(1.5))
it('ranks deterministically without mutation',()=>{const x=[{project:'z',durationSeconds:2},{project:'a',durationSeconds:2},{project:'b',durationSeconds:3}];expect(rankDurations(x,i=>i.project).map(i=>i.project)).toEqual(['b','a','z']);expect(x[0].project).toBe('z')})
it('sorts daily values chronologically',()=>expect(sortCodingDaily([{date:'2026-09-02'},{date:'2026-09-01'}]).map(x=>x.date)).toEqual(['2026-09-01','2026-09-02']))
