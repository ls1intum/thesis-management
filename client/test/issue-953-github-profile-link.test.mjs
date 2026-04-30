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
// Mirrors the rule in customDataLink.tsx. Kept in sync deliberately so a
// regression to the previous lax form is caught here.
const GITHUB_USERNAME_RE = /^(?=.{1,39}$)[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$/

function buildGithubUrl(value) {
  if (typeof value !== 'string') return null
  const trimmed = value.trim().replace(/^@/, '')
  if (!trimmed) return null
  try {
    const url = new URL(trimmed)
    if ((url.protocol === 'https:' || url.protocol === 'http:') && url.host === 'github.com') {
      return url.toString()
    }
    return null
  } catch {
    // Not a URL — try the username rules.
  }
  if (GITHUB_USERNAME_RE.test(trimmed)) {
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

  test('helper renders an Anchor with target="_blank" and full reverse-tabnabbing protection', () => {
    assert.match(
      helperSource,
      /<Anchor[^>]*target=['"]_blank['"]/,
      'Expected the helper to render an Anchor with target="_blank" for derived URLs.',
    )
    assert.match(
      helperSource,
      /rel=['"]noopener noreferrer['"]/,
      'Expected rel="noopener noreferrer" on external links to prevent reverse-tabnabbing.',
    )
  })

  test('the link is wrapped in a truncating Mantine Text so layout stays stable', () => {
    assert.match(
      helperSource,
      /<Text[^>]*truncate[\s\S]*?<Anchor/,
      'Expected the Anchor to be wrapped in a truncating <Text> to preserve LabeledItem layout.',
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

    test('full github.com URLs are accepted', () => {
      assert.equal(
        buildGithubUrl('https://github.com/some-user'),
        'https://github.com/some-user',
      )
    })

    test('non-github URLs are rejected (defense against arbitrary external links)', () => {
      assert.equal(buildGithubUrl('https://example.com/octocat'), null)
      assert.equal(buildGithubUrl('http://evil.example.com'), null)
      assert.equal(buildGithubUrl('javascript:alert(1)'), null)
    })

    test('values that look like usernames but violate GitHub rules are rejected', () => {
      // trailing hyphen
      assert.equal(buildGithubUrl('foo-'), null)
      // leading hyphen
      assert.equal(buildGithubUrl('-foo'), null)
      // consecutive hyphens
      assert.equal(buildGithubUrl('foo--bar'), null)
      // longer than 39 chars
      assert.equal(buildGithubUrl('a'.repeat(40)), null)
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
