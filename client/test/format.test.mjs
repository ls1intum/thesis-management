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
    const noTime = formatDateForLocale(Date.UTC(2026, 1, 12, 12, 0, 0), 'en-US', {
      withTime: false,
    })
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

  test('production formatDate in src/utils/format.ts uses dateStyle medium + withTime-gated timeStyle', async () => {
    // The local re-implementation above only catches regressions in the test
    // copy. Pin the production source so a revert to numeric/2-digit options
    // (e.g. month: '2-digit') fails this test as well.
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(join(ROOT, 'src/utils/format.ts'), 'utf8')
    assert.match(source, /export function formatDate\(/)
    assert.match(source, /dateStyle:\s*'medium'/)
    assert.match(source, /withTime\s*\?\s*\{\s*timeStyle:\s*'short'\s*\}\s*:\s*\{\s*\}/)
    // Guard against accidental regressions to the previously-ambiguous options.
    assert.doesNotMatch(source, /year:\s*'2-digit'/)
    assert.doesNotMatch(source, /month:\s*'2-digit'/)
  })
})

describe('ensureAbsoluteLinkHref — issue #802 (rich-text editor link normalization)', () => {
  // The DocumentEditor calls `ensureAbsoluteLinkHref` (defined in
  // src/utils/format.ts) from inside the SmartLink extension's overridden
  // `setLink` command, so a schemeless href the user types into the toolbar's
  // link popover (e.g. "example.com") is stored as the absolute
  // "https://example.com" rather than as a relative path under the current
  // page. We re-implement the function below to keep this test pure-Node — the
  // assertions document the exact behavior the production override depends on.
  function ensureAbsoluteLinkHref(href) {
    if (!href) return href
    const trimmed = href.trim()
    if (!trimmed) return trimmed
    if (trimmed.startsWith('#') || trimmed.startsWith('/')) return trimmed
    if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) return trimmed
    return `https://${trimmed}`
  }

  test('schemeless host gets https prefix (regression for #802)', () => {
    assert.equal(ensureAbsoluteLinkHref('example.com'), 'https://example.com')
    assert.equal(ensureAbsoluteLinkHref('liiiink.de'), 'https://liiiink.de')
    assert.equal(ensureAbsoluteLinkHref('host.tld/path?q=1'), 'https://host.tld/path?q=1')
  })

  test('explicit http(s) scheme is preserved as-is', () => {
    assert.equal(ensureAbsoluteLinkHref('http://insecure.example'), 'http://insecure.example')
    assert.equal(ensureAbsoluteLinkHref('https://example.com/x'), 'https://example.com/x')
    assert.equal(ensureAbsoluteLinkHref('HTTPS://Example.com'), 'HTTPS://Example.com')
  })

  test('non-http schemes (mailto:, tel:, ftp:) are not corrupted', () => {
    assert.equal(ensureAbsoluteLinkHref('mailto:user@example.com'), 'mailto:user@example.com')
    assert.equal(ensureAbsoluteLinkHref('tel:+49123456789'), 'tel:+49123456789')
    assert.equal(ensureAbsoluteLinkHref('ftp://files.example.com'), 'ftp://files.example.com')
  })

  test('intentionally relative hrefs are not modified', () => {
    // Anchor-only and absolute-path hrefs are legitimate within rendered content
    // (e.g. table-of-contents anchors) and must not gain a schemeless host part.
    assert.equal(ensureAbsoluteLinkHref('#section'), '#section')
    assert.equal(ensureAbsoluteLinkHref('/relative/path'), '/relative/path')
  })

  test('empty / nullish hrefs are returned unchanged', () => {
    assert.equal(ensureAbsoluteLinkHref(''), '')
    assert.equal(ensureAbsoluteLinkHref(undefined), undefined)
    assert.equal(ensureAbsoluteLinkHref(null), null)
  })

  test('whitespace-padded input is trimmed before normalization', () => {
    // A user pasting "  example.com " into the link popover must not produce
    // "https://  example.com " (which the browser then resolves as a broken URL).
    assert.equal(ensureAbsoluteLinkHref('  example.com  '), 'https://example.com')
    assert.equal(ensureAbsoluteLinkHref('\t#anchor\n'), '#anchor')
    assert.equal(ensureAbsoluteLinkHref(' /relative '), '/relative')
    assert.equal(ensureAbsoluteLinkHref('  https://example.com  '), 'https://example.com')
    assert.equal(ensureAbsoluteLinkHref('   '), '')
  })

  test('production ensureAbsoluteLinkHref in src/utils/format.ts trims input and prefixes https', async () => {
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(join(ROOT, 'src/utils/format.ts'), 'utf8')
    assert.match(source, /export function ensureAbsoluteLinkHref\(/)
    assert.match(source, /href\.trim\(\)/)
    assert.match(source, /`https:\/\/\$\{trimmed\}`/)
  })

  test('autolink + paste paths still rely on Tiptap defaultProtocol via linkifyjs', async () => {
    // linkifyjs is what Tiptap uses internally for both autolinking and the
    // paste handler. Verifying it here ensures that `defaultProtocol: 'https'`
    // continues to deliver schemeless-to-https promotion for those code paths,
    // which the toolbar override does not cover.
    const { tokenize } = await import('linkifyjs')
    const tokens = tokenize('Visit example.com for more')
    const url = tokens.find((t) => t.t === 'url')
    assert.ok(url, 'expected a URL token to be detected in the input')
    assert.equal(url.toHref('https'), 'https://example.com')
  })

  test('linkifyjs preserves an explicit http:// scheme without rewriting', async () => {
    const { tokenize } = await import('linkifyjs')
    const tokens = tokenize('Visit http://insecure.example for more')
    const url = tokens.find((t) => t.t === 'url')
    assert.ok(url)
    assert.equal(url.toHref('https'), 'http://insecure.example')
  })

  test('DocumentEditor wires both halves of the fix (SmartLink + defaultProtocol)', async () => {
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(
      join(ROOT, 'src/components/DocumentEditor/DocumentEditor.tsx'),
      'utf8',
    )
    // SmartLink override of setLink (covers the toolbar popover path).
    assert.match(source, /SmartLink\s*=\s*Link\.extend\(/)
    assert.match(source, /ensureAbsoluteLinkHref\(attributes\.href\)/)
    // defaultProtocol option on the registered extension (covers autolink + paste).
    assert.match(source, /SmartLink\.configure\(\s*\{\s*defaultProtocol:\s*'https'\s*\}\s*\)/)
  })
})
