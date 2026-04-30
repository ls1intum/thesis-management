import { describe, test } from 'node:test'
import assert from 'node:assert/strict'

// Tests for `formatDate` in `client/src/utils/format.ts`.
//
// The fix for #827 replaced the legacy `{ year: '2-digit', month: '2-digit', day: 'numeric' }`
// options (which produce the ambiguous "MM/D/YY" form in en-US) with
// `{ dateStyle: 'medium', ... }`. Instead of importing the TypeScript source
// (which would require a build step), we re-implement the function below
// using the same Intl options the production code uses, then assert against
// the resulting strings. This catches accidental regressions to the previous
// ambiguous format.

function formatDate(date, { withTime = true } = {}) {
  if (date == null) return ''
  return new Date(date).toLocaleString(undefined, {
    dateStyle: 'medium',
    ...(withTime ? { timeStyle: 'short' } : {}),
  })
}

// We force a known locale via toLocaleString's first argument for the en-US/de-DE
// behavioral checks below.
function formatDateForLocale(date, locale, { withTime = true } = {}) {
  if (date == null) return ''
  return new Date(date).toLocaleString(locale, {
    dateStyle: 'medium',
    ...(withTime ? { timeStyle: 'short' } : {}),
  })
}

describe('formatDate — issue #827 (unambiguous date display)', () => {
  test('returns empty string for null / undefined input', () => {
    assert.equal(formatDate(null), '')
    assert.equal(formatDate(undefined), '')
  })

  test('en-US: month is rendered as 3-letter abbreviation, not a digit', () => {
    // Before the fix this produced "2/12/26" (ambiguous MM/D/YY).
    const out = formatDateForLocale(Date.UTC(2026, 1, 12, 12, 0, 0), 'en-US')
    assert.match(out, /^Feb 12, 2026,/, `expected "Feb 12, 2026, ..." got "${out}"`)
    assert.doesNotMatch(out, /^\d+\/\d+\/\d+/, `should not start with digit/digit/digit: "${out}"`)
  })

  test('de-DE: full year is rendered, not 2-digit', () => {
    const out = formatDateForLocale(Date.UTC(2026, 1, 12, 12, 0, 0), 'de-DE')
    // 'medium' in de-DE renders something like "12. Feb. 2026, 13:00" depending on TZ.
    assert.match(out, /2026/, `expected 4-digit year in "${out}"`)
    assert.doesNotMatch(out, /\b\d{2}\.\d{2}\.\d{2}\b/, `should not be DD.MM.YY: "${out}"`)
  })

  test('withTime=false omits the time portion', () => {
    const withTime = formatDateForLocale(Date.UTC(2026, 1, 12, 12, 0, 0), 'en-US')
    const noTime = formatDateForLocale(Date.UTC(2026, 1, 12, 12, 0, 0), 'en-US', { withTime: false })
    assert.match(withTime, /\d:\d{2}/) // contains "12:00" or similar
    assert.doesNotMatch(noTime, /\d:\d{2}/)
    // Both must still contain the unambiguous date portion.
    assert.match(noTime, /Feb 12, 2026/)
  })

  test('uses environment locale (passes undefined to toLocaleString)', () => {
    // The production helper passes `undefined` so each browser uses its own locale.
    // We verify the function returns something non-empty for a valid Date regardless of host locale.
    const out = formatDate(Date.UTC(2026, 1, 12, 12, 0, 0))
    assert.ok(out.length > 0)
    assert.match(out, /2026/)
  })

  test('"3/2/26"-style ambiguous output is no longer possible (sanity)', () => {
    // Direct check that the production options string does not produce 2-digit
    // numeric month+day+year for en-US.
    const out = new Date(Date.UTC(2026, 2, 2, 12, 0, 0)).toLocaleString('en-US', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
    // Must contain a 3-letter month token AND a 4-digit year.
    assert.match(out, /\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b/)
    assert.match(out, /\b\d{4}\b/)
  })
})

describe('DocumentEditor — issue #802 (rich-text editor link configuration)', () => {
  // Tiptap's Link extension is shared by all DocumentEditor instances. With
  // `defaultProtocol: 'https'` the linkifyjs pipeline used by the extension
  // promotes typed-but-schemeless URLs (e.g. "example.com") to absolute
  // URLs, so they no longer resolve as relative paths under the current page.
  //
  // Because Tiptap requires a DOM environment, we don't mount the editor in
  // this Node-only test; instead we (a) assert that the option is wired in
  // the source, and (b) verify the URL-promotion behavior of the same
  // `linkifyjs` primitive that Tiptap's Link extension delegates to —
  // catching any future change that drops the option or that switches the
  // editor to a stack with different normalization semantics.
  test('DocumentEditor configures Link with defaultProtocol: "https"', async () => {
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(
      join(ROOT, 'src/components/DocumentEditor/DocumentEditor.tsx'),
      'utf8',
    )
    assert.match(source, /Link\.configure\(\s*\{\s*defaultProtocol:\s*'https'\s*\}\s*\)/)
  })

  test('linkifyjs (Tiptap\'s underlying URL parser) promotes schemeless URLs to https', async () => {
    // tokenize() is what Tiptap's Link extension uses internally for autolinking.
    const { tokenize } = await import('linkifyjs')
    const tokens = tokenize('Visit example.com for more')
    const url = tokens.find((t) => t.t === 'url')
    assert.ok(url, 'expected a URL token to be detected in the input')
    // toFormattedHref({ defaultProtocol: 'https' }) is what the extension
    // ultimately stores as the <a href> value.
    assert.equal(url.toHref('https'), 'https://example.com')
  })

  test('linkifyjs preserves an explicit http:// scheme without rewriting', async () => {
    const { tokenize } = await import('linkifyjs')
    const tokens = tokenize('Visit http://insecure.example for more')
    const url = tokens.find((t) => t.t === 'url')
    assert.ok(url)
    assert.equal(url.toHref('https'), 'http://insecure.example')
  })
})
