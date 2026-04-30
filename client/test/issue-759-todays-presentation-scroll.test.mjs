import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #759.
//
// Today's presentation was easy to miss because the page rendered the
// calendar starting from whichever date came first in the loaded data,
// not from "today". The fix adds a useEffect that scrolls to today's
// date heading after presentations load, falling back to the next
// upcoming day, then the first available date.
//
// We verify two things:
//   1) The component contains a useEffect that depends on `presentations`
//      and resolves a target date in the documented priority order
//      (today → next upcoming → first available).
//   2) The fallback target-selection logic behaves as documented.

const here = dirname(fileURLToPath(import.meta.url))
const pagePath = resolve(
  here,
  '../src/pages/PresentationOverviewPage/PresentationOverviewPage.tsx',
)
const source = readFileSync(pagePath, 'utf8')

// Re-implementation of the target-selection rule. Kept in sync with the
// useEffect in PresentationOverviewPage.
function pickTargetDate(today, dates) {
  const sorted = [...dates].sort()
  return (
    sorted.find((d) => d === today) ??
    sorted.find((d) => d >= today) ??
    sorted[0]
  )
}

describe('PresentationOverviewPage — issue #759 (auto-scroll to today)', () => {
  test('component installs a useEffect that scrolls to today', () => {
    // Look for the marker dayjs().format('YYYY-MM-DD') referencing today
    assert.match(
      source,
      /dayjs\(\)\.format\(['"]YYYY-MM-DD['"]\)/,
      'Expected the auto-scroll useEffect to compute today via dayjs().format("YYYY-MM-DD").',
    )
  })

  test('useEffect calls scrollTo with the resolved target date', () => {
    assert.match(
      source,
      /scrollTo\(target\)/,
      'Expected the useEffect to call scrollTo(target) once a target date is resolved.',
    )
  })

  test('the fallback ladder prefers today, then next upcoming, then earliest', () => {
    // The find chain should appear in the source - capture key fragments.
    assert.match(
      source,
      /sortedDates\.find\(\(date\)\s*=>\s*date\s*===\s*today\)/,
      'Expected the first preference to be the date matching today exactly.',
    )
    assert.match(
      source,
      /sortedDates\.find\(\(date\)\s*=>\s*!dayjs\(date\)\.isBefore\(dayjs\(today\)\)\)/,
      'Expected the second preference to be the next non-past date.',
    )
  })

  test('the useEffect runs when presentations change', () => {
    // The dep array of the auto-scroll effect must include `presentations`.
    const effectDep = source.match(
      /requestAnimationFrame[\s\S]*?\}\s*,\s*\[(presentations[^\]]*)\]\s*\)/,
    )
    assert.ok(effectDep, 'Could not locate the auto-scroll useEffect dep array.')
  })

  describe('target-date selection rules', () => {
    const today = '2026-04-30'

    test('returns today when today has presentations', () => {
      assert.equal(
        pickTargetDate(today, ['2026-04-29', today, '2026-05-02']),
        today,
      )
    })

    test('returns the next upcoming date when today has no presentation', () => {
      assert.equal(
        pickTargetDate(today, ['2026-04-15', '2026-05-02', '2026-05-10']),
        '2026-05-02',
      )
    })

    test('falls back to the earliest date when everything is in the past', () => {
      assert.equal(
        pickTargetDate(today, ['2026-04-10', '2026-04-15']),
        '2026-04-10',
      )
    })

    test('returns undefined when there are no dates', () => {
      assert.equal(pickTargetDate(today, []), undefined)
    })
  })
})
