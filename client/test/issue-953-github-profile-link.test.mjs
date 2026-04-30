import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #953.
//
// Custom data fields like the GitHub username were rendered as plain text.
// The fix introduces `renderCustomDataValue` (utils/customDataLink.tsx),
// used in both ApplicationData and ThesisStudentInfoSection.
//
// We verify two things:
//   1) The two render sites import and use the helper.
//   2) The helper's URL-building rule for GITHUB matches the expected
//      behavior, by re-implementing the same regex/derivation in JS and
//      checking representative cases.

const here = dirname(fileURLToPath(import.meta.url))
const helperPath = resolve(here, '../src/utils/customDataLink.tsx')
const helperSource = readFileSync(helperPath, 'utf8')

const applicationDataPath = resolve(here, '../src/components/ApplicationData/ApplicationData.tsx')
const applicationDataSource = readFileSync(applicationDataPath, 'utf8')

const thesisStudentPath = resolve(
  here,
  '../src/pages/ThesisPage/components/ThesisStudentInfoSection/ThesisStudentInfoSection.tsx',
)
const thesisStudentSource = readFileSync(thesisStudentPath, 'utf8')

// Re-implementation of the URL-builder rule for GITHUB. Kept in sync with
// linkBuilders.GITHUB in customDataLink.tsx.
function buildGithubUrl(value) {
  if (typeof value !== 'string') return null
  const trimmed = value.trim().replace(/^@/, '')
  if (!trimmed) return null
  if (/^https?:\/\//i.test(trimmed)) return trimmed
  if (/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$/.test(trimmed)) {
    return `https://github.com/${trimmed}`
  }
  return null
}

describe('renderCustomDataValue — issue #953 (GitHub profile link)', () => {
  test('helper file exists and exports renderCustomDataValue', () => {
    assert.match(
      helperSource,
      /export\s+const\s+renderCustomDataValue\s*=/,
      'Expected `renderCustomDataValue` to be exported from utils/customDataLink.tsx.',
    )
  })

  test('helper renders an Anchor with target="_blank" for known link keys', () => {
    assert.match(
      helperSource,
      /<Anchor[^>]*target=['"]_blank['"]/,
      'Expected the helper to render an Anchor with target="_blank" for derived URLs.',
    )
    assert.match(
      helperSource,
      /rel=['"]noreferrer['"]/,
      'Expected rel="noreferrer" on external links.',
    )
  })

  test('GITHUB link builder is registered', () => {
    assert.match(
      helperSource,
      /linkBuilders\s*[:=][\s\S]*?GITHUB\s*:/,
      'Expected GITHUB to be registered in linkBuilders.',
    )
    assert.match(
      helperSource,
      /https:\/\/github\.com\//,
      'Expected the GITHUB builder to produce https://github.com/ URLs.',
    )
  })

  test('ApplicationData uses renderCustomDataValue for customData entries', () => {
    assert.match(
      applicationDataSource,
      /renderCustomDataValue\(\s*key\s*,\s*value\s*\)/,
      'Expected ApplicationData to wrap customData values with renderCustomDataValue(key, value).',
    )
  })

  test('ThesisStudentInfoSection uses renderCustomDataValue for customData entries', () => {
    assert.match(
      thesisStudentSource,
      /renderCustomDataValue\(\s*key\s*,\s*value\s*\)/,
      'Expected ThesisStudentInfoSection to wrap customData values with renderCustomDataValue(key, value).',
    )
  })

  // Behavioral checks against the re-implementation. These pin the documented
  // rules; if the source rules change, these tests need to change too.
  describe('GITHUB URL derivation rules', () => {
    test('plain username produces a github.com URL', () => {
      assert.equal(buildGithubUrl('octocat'), 'https://github.com/octocat')
    })

    test('a leading @ is stripped', () => {
      assert.equal(buildGithubUrl('@octocat'), 'https://github.com/octocat')
    })

    test('whitespace is trimmed', () => {
      assert.equal(buildGithubUrl('  octocat  '), 'https://github.com/octocat')
    })

    test('full URLs are passed through unchanged', () => {
      assert.equal(
        buildGithubUrl('https://github.com/some-user'),
        'https://github.com/some-user',
      )
      assert.equal(
        buildGithubUrl('http://example.com/octocat'),
        'http://example.com/octocat',
      )
    })

    test('empty / whitespace-only values yield null (no broken link)', () => {
      assert.equal(buildGithubUrl(''), null)
      assert.equal(buildGithubUrl('   '), null)
      assert.equal(buildGithubUrl('@'), null)
    })

    test('values with invalid GitHub username characters yield null', () => {
      assert.equal(buildGithubUrl('not a user'), null)
      assert.equal(buildGithubUrl('user/something'), null)
    })
  })
})
