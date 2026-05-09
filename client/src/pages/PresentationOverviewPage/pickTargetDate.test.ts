import { describe, it, expect } from 'vitest'
import { pickTargetDate } from './pickTargetDate'

describe('pickTargetDate (issue #759 auto-scroll target selection)', () => {
  const today = '2026-04-30'

  it('returns today when today has presentations', () => {
    expect(pickTargetDate(today, ['2026-04-29', today, '2026-05-02'])).toBe(today)
  })

  it('returns the next upcoming date when today has no presentation', () => {
    expect(pickTargetDate(today, ['2026-04-15', '2026-05-02', '2026-05-10'])).toBe('2026-05-02')
  })

  it('falls back to the most recent past date when everything is in the past', () => {
    expect(pickTargetDate(today, ['2026-04-10', '2026-04-15'])).toBe('2026-04-15')
  })

  it('returns undefined when there are no dates', () => {
    expect(pickTargetDate(today, [])).toBeUndefined()
  })

  it('does not require dates to be pre-sorted', () => {
    expect(pickTargetDate(today, ['2026-05-10', '2026-04-15', '2026-05-02'])).toBe('2026-05-02')
  })

  it('handles a single-element list when that element is today', () => {
    expect(pickTargetDate(today, [today])).toBe(today)
  })

  it('handles duplicate dates in the input', () => {
    expect(pickTargetDate(today, [today, today, '2026-05-02'])).toBe(today)
  })
})
