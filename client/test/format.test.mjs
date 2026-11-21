import { describe, test } from 'node:test'
import assert from 'node:assert/strict'

// Tests for the date helpers `formatDate`, `formatTime` and `formatDateTime` in
// `client/src/core/utils/format.ts`.
//
// The fix for #827 removed ambiguous numeric dates such as "2/12/26" (MM/D/YY in
// en-US, DD.MM.YY in de-DE). Dates are now rendered in one locale-neutral layout:
// `YYYY-MM-DD`, `HH:mm` (24 h) and `YYYY-MM-DD HH:mm`. The helpers are built from
// the Date getters, never from `toLocale*` / `Intl`, so the output cannot depend on
// the browser's locale. Instead of importing the TypeScript source (which would
// require a build step), we re-implement the helpers below and pin the production
// source in the last test, so a revert to locale-dependent formatting fails here too.

const pad2 = (value) => value.toString().padStart(2, '0')
const pad4 = (value) => value.toString().padStart(4, '0')

function toValidDate(date) {
  if (date === undefined || date === null) return null
  if (typeof date === 'string') {
    const dateOnly = date.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (dateOnly) {
      const [year, month, day] = [Number(dateOnly[1]), Number(dateOnly[2]), Number(dateOnly[3])]
      const item = new Date(0)
      item.setFullYear(year, month - 1, day)
      item.setHours(0, 0, 0, 0)
      if (item.getFullYear() !== year || item.getMonth() !== month - 1 || item.getDate() !== day) {
        return null
      }
      return item
    }
  }
  const item = new Date(date)
  return Number.isNaN(item.getTime()) ? null : item
}

function formatDate(date) {
  const item = toValidDate(date)
  if (item === null) return ''
  return `${pad4(item.getFullYear())}-${pad2(item.getMonth() + 1)}-${pad2(item.getDate())}`
}

function formatTime(date) {
  const item = toValidDate(date)
  if (item === null) return ''
  return `${pad2(item.getHours())}:${pad2(item.getMinutes())}`
}

function formatDateTime(date) {
  const item = toValidDate(date)
  if (item === null) return ''
  return `${formatDate(item)} ${formatTime(item)}`
}

describe('formatDate / formatTime / formatDateTime — issue #827 (unambiguous, locale-neutral dates)', () => {
  // Built from local-time components so the expectations hold in every time zone.
  const morning = new Date(2026, 1, 12, 9, 5)
  const afternoon = new Date(2026, 1, 12, 13, 45)

  test('return an empty string for null, undefined and invalid input', () => {
    for (const helper of [formatDate, formatTime, formatDateTime]) {
      assert.equal(helper(null), '')
      assert.equal(helper(undefined), '')
      assert.equal(helper('not a date'), '')
      assert.equal(helper(new Date(Number.NaN)), '')
    }
  })

  test('formatDate renders YYYY-MM-DD, never an ambiguous numeric form', () => {
    // Before the fix this produced "2/12/26" (MM/D/YY) in en-US.
    assert.equal(formatDate(morning), '2026-02-12')
    assert.match(formatDate(morning), /^\d{4}-\d{2}-\d{2}$/)
  })

  test('formatTime renders 24-hour HH:mm with zero padding', () => {
    assert.equal(formatTime(morning), '09:05')
    assert.equal(formatTime(afternoon), '13:45') // not "1:45 PM"
  })

  test('formatDateTime renders "YYYY-MM-DD HH:mm"', () => {
    assert.equal(formatDateTime(morning), '2026-02-12 09:05')
    assert.equal(formatDateTime(afternoon), '2026-02-12 13:45')
  })

  test('does not call any locale-dependent Date API', () => {
    const originals = {}
    for (const name of ['toLocaleString', 'toLocaleDateString', 'toLocaleTimeString']) {
      originals[name] = Date.prototype[name]
      Date.prototype[name] = () => {
        throw new Error(`${name} must not be used for date formatting`)
      }
    }
    try {
      assert.equal(formatDateTime(afternoon), '2026-02-12 13:45')
    } finally {
      Object.assign(Date.prototype, originals)
    }
  })

  test('date-only strings are read as local calendar dates (no time-zone shift)', () => {
    // `new Date("2026-02-12")` is UTC midnight, which is still 11 Feb west of UTC.
    assert.equal(formatDate('2026-02-12'), '2026-02-12')
    assert.equal(formatDateTime('2026-02-12'), '2026-02-12 00:00')
  })

  test('impossible date-only strings are rejected instead of rolling over', () => {
    assert.equal(formatDate('2026-02-30'), '')
    assert.equal(formatDate('2026-13-01'), '')
  })

  test('years below 1000 are zero-padded to four digits', () => {
    const early = new Date(0)
    early.setFullYear(999, 0, 1)
    early.setHours(0, 0, 0, 0)
    assert.equal(formatDate(early), '0999-01-01')
  })

  test('production helpers in src/core/utils/format.ts are built without locale APIs', async () => {
    // The local re-implementation above only catches regressions in the test
    // copy. Pin the production source so a revert to `toLocaleString` / `Intl`
    // (or to the old `withTime` option) fails this test as well.
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(join(ROOT, 'src/core/utils/format.ts'), 'utf8')
    assert.match(source, /export function formatDate\(/)
    assert.match(source, /export function formatTime\(/)
    assert.match(source, /export function formatDateTime\(/)
    for (const getter of ['getFullYear', 'getMonth', 'getDate', 'getHours', 'getMinutes']) {
      assert.match(source, new RegExp(`\\.${getter}\\(\\)`), `expected ${getter}() in format.ts`)
    }
    assert.doesNotMatch(source, /toLocale(Date|Time)?String/)
    assert.doesNotMatch(source, /Intl\.DateTimeFormat/)
    assert.doesNotMatch(source, /dateStyle|timeStyle/)
    assert.doesNotMatch(source, /withTime/)
    // Guard against a return to the previously-ambiguous options.
    assert.doesNotMatch(source, /year:\s*'2-digit'/)
    assert.doesNotMatch(source, /month:\s*'2-digit'/)
  })
})

describe('ensureAbsoluteLinkHref — issue #802 (rich-text editor link normalization)', () => {
  // The DocumentEditor calls `ensureAbsoluteLinkHref` (defined in
  // src/core/utils/format.ts) from inside the SmartLink extension's overridden
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

  test('production ensureAbsoluteLinkHref in src/core/utils/format.ts trims input and prefixes https', async () => {
    const { readFileSync } = await import('node:fs')
    const { join, dirname } = await import('node:path')
    const { fileURLToPath } = await import('node:url')
    const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
    const source = readFileSync(join(ROOT, 'src/core/utils/format.ts'), 'utf8')
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
      join(ROOT, 'src/core/components/DocumentEditor/DocumentEditor.tsx'),
      'utf8',
    )
    // SmartLink override of setLink (covers the toolbar popover path).
    assert.match(source, /SmartLink\s*=\s*Link\.extend\(/)
    assert.match(source, /ensureAbsoluteLinkHref\(attributes\.href\)/)
    // defaultProtocol option on the registered extension (covers autolink + paste).
    assert.match(source, /SmartLink\.configure\(\s*\{\s*defaultProtocol:\s*'https'\s*\}\s*\)/)
  })
})
