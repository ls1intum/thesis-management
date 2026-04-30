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
// date heading after presentations load.
//
// Verified invariants:
//   1) The component installs a useEffect depending on `presentations`
//      that resolves a target date in the documented priority order
//      (today → next upcoming → most recent past).
//   2) The fallback target-selection logic behaves as documented.
//   3) An auto-scroll guard keeps the effect from re-targeting the
//      viewport on every onDelete/onUpdate-driven re-render.

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
    sorted[sorted.length - 1]
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

  test('the fallback ladder prefers today, then next upcoming, then most-recent past', () => {
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
    assert.match(
      source,
      /sortedDates\[sortedDates\.length\s*-\s*1\]/,
      'Expected the final fallback to be the most recent date (sortedDates[last]), not the earliest.',
    )
  })

  test('the useEffect re-runs when presentations OR the selected group change', () => {
    // The dep array of the auto-scroll effect must include both
    // `presentations` and `selectedGroup?.id` so changing the selected
    // research group re-targets the scroll.
    const effectDep = source.match(
      /requestAnimationFrame[\s\S]*?\}\s*,\s*\[(presentations[^\]]*)\]\s*\)/,
    )
    assert.ok(effectDep, 'Could not locate the auto-scroll useEffect dep array.')
    assert.match(
      effectDep[1],
      /selectedGroup\?\.id/,
      'Expected the dep array to also include `selectedGroup?.id`.',
    )
  })

  test('an auto-scroll guard prevents re-targeting on user mutations', () => {
    // The fix must record which groupId we last scrolled for, so onDelete /
    // onUpdate re-renders (which produce a new presentations Map identity)
    // do not yank the viewport back to today.
    assert.match(
      source,
      /lastScrolledGroupId\s*=\s*useRef/,
      'Expected a ref tracking the last group the auto-scroll fired for.',
    )
    assert.match(
      source,
      /lastScrolledGroupId\.current\s*===\s*\(selectedGroup\?\.id\s*\?\?\s*null\)/,
      'Expected the effect to bail out when the same group is already scroll-targeted.',
    )
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

    test('falls back to the most recent past date when everything is in the past', () => {
      assert.equal(
        pickTargetDate(today, ['2026-04-10', '2026-04-15']),
        '2026-04-15',
      )
    })

    test('returns undefined when there are no dates', () => {
      assert.equal(pickTargetDate(today, []), undefined)
    })
  })
})
