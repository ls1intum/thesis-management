import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #729.
//
// PresentationOverviewPage installed a global wheel listener that manually
// scrolled an inner ref. On mobile this caused content to slide under the
// sticky page header because the inner container has no offset for it.
// The fix skips the custom wheel handler when below the `md` breakpoint
// (`isSmaller`), letting the page scroll natively on mobile.

const here = dirname(fileURLToPath(import.meta.url))
const pagePath = resolve(
  here,
  '../src/pages/PresentationOverviewPage/PresentationOverviewPage.tsx',
)
const source = readFileSync(pagePath, 'utf8')

describe('PresentationOverviewPage — issue #729 (mobile scroll guard)', () => {
  test('isSmaller is consulted before installing the custom wheel handler', () => {
    // Find the useEffect that registers the wheel listener and check that
    // it early-returns when isSmaller is true.
    const wheelEffectRegex =
      /useEffect\(\(\)\s*=>\s*\{[\s\S]*?if\s*\(\s*isSmaller\s*\)\s*\{[\s\S]*?return[\s\S]*?\}[\s\S]*?addEventListener\(['"]wheel['"]/
    assert.match(
      source,
      wheelEffectRegex,
      'Expected the wheel-listener useEffect to early-return when isSmaller is true (mobile breakpoint).',
    )
  })

  test('isSmaller is included in the wheel useEffect dep array', () => {
    // The dep array must contain isSmaller so the effect re-runs on viewport changes.
    // Capture the closing of the dep array right after the wheel useEffect.
    const depRegex =
      /addEventListener\(['"]wheel['"][\s\S]*?removeEventListener\(['"]wheel['"][\s\S]*?\}\s*,\s*\[([^\]]*)\]\s*\)/
    const match = source.match(depRegex)
    assert.ok(match, 'Could not locate the wheel useEffect dep array.')
    assert.match(
      match[1],
      /isSmaller/,
      'Expected the wheel useEffect dep array to include `isSmaller`.',
    )
  })
})
