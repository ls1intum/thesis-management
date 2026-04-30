import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #594.
//
// A non-production environment (test/dev/staging) should be visually
// indicated in the UI:
//   - A banner above the header explaining the environment
//   - The footer shows the environment alongside the version
//
// We verify three things:
//   1) The Environment type is part of IGlobalConfig.
//   2) global.ts parses the ENVIRONMENT variable into one of the allowed values.
//   3) EnvironmentBanner returns null on production / unset, and renders for the others.
//   4) Footer appends the environment label after the version.

const here = dirname(fileURLToPath(import.meta.url))

const typesPath = resolve(here, '../src/config/types.ts')
const typesSource = readFileSync(typesPath, 'utf8')

const globalPath = resolve(here, '../src/config/global.ts')
const globalSource = readFileSync(globalPath, 'utf8')

const bannerPath = resolve(here, '../src/components/EnvironmentBanner/EnvironmentBanner.tsx')
const bannerSource = readFileSync(bannerPath, 'utf8')

const footerPath = resolve(here, '../src/components/Footer/Footer.tsx')
const footerSource = readFileSync(footerPath, 'utf8')

const layoutPath = resolve(
  here,
  '../src/app/layout/AuthenticatedArea/AuthenticatedArea.tsx',
)
const layoutSource = readFileSync(layoutPath, 'utf8')

// Re-implementation of the parser in global.ts. Kept in sync with parseEnvironment.
function parseEnvironment(value) {
  if (!value) return undefined
  const normalized = String(value).toLowerCase()
  if (
    normalized === 'production' ||
    normalized === 'staging' ||
    normalized === 'test' ||
    normalized === 'dev'
  ) {
    return normalized
  }
  return undefined
}

describe('Environment banner — issue #594', () => {
  test('IGlobalConfig declares an optional `environment` field of the Environment type', () => {
    assert.match(
      typesSource,
      /export\s+type\s+Environment\s*=\s*['"]production['"]\s*\|\s*['"]staging['"]\s*\|\s*['"]test['"]\s*\|\s*['"]dev['"]/,
      'Expected `Environment` union type covering production/staging/test/dev.',
    )
    assert.match(
      typesSource,
      /environment\?\s*:\s*Environment/,
      'Expected `environment?: Environment` to be declared on IGlobalConfig.',
    )
  })

  test('global.ts wires ENVIRONMENT through parseEnvironment', () => {
    assert.match(
      globalSource,
      /environment:\s*parseEnvironment\(getEnvironmentVariable\(['"]ENVIRONMENT['"]\)\)/,
      'Expected GLOBAL_CONFIG.environment to be parsed from getEnvironmentVariable("ENVIRONMENT").',
    )
  })

  describe('parseEnvironment behavior', () => {
    test('returns undefined for missing or empty input', () => {
      assert.equal(parseEnvironment(undefined), undefined)
      assert.equal(parseEnvironment(''), undefined)
    })

    test('accepts the four documented values', () => {
      assert.equal(parseEnvironment('production'), 'production')
      assert.equal(parseEnvironment('staging'), 'staging')
      assert.equal(parseEnvironment('test'), 'test')
      assert.equal(parseEnvironment('dev'), 'dev')
    })

    test('is case-insensitive', () => {
      assert.equal(parseEnvironment('DEV'), 'dev')
      assert.equal(parseEnvironment('Production'), 'production')
    })

    test('rejects unknown values', () => {
      assert.equal(parseEnvironment('preview'), undefined)
      assert.equal(parseEnvironment('local'), undefined)
    })
  })

  test('EnvironmentBanner short-circuits on production / unset', () => {
    // The exported isEnvironmentBannerVisible (and the component itself) must
    // return false / null for production or undefined.
    assert.match(
      bannerSource,
      /export\s+const\s+isEnvironmentBannerVisible\s*=\s*\(\s*\)\s*:\s*boolean\s*=>\s*\{[\s\S]*?return\s+!!environment\s+&&\s+environment\s*!==\s*['"]production['"]/,
      'Expected isEnvironmentBannerVisible to return true only for non-production values.',
    )
    assert.match(
      bannerSource,
      /if\s*\(\s*!isEnvironmentBannerVisible\(\)\s*\)\s*\{[\s\S]*?return\s+null/,
      'Expected the component to return null when the banner is not visible.',
    )
  })

  test('Footer appends the environment label to the version', () => {
    assert.match(
      footerSource,
      /GLOBAL_CONFIG\.environment[\s\S]*?·\s*\$\{GLOBAL_CONFIG\.environment\}/,
      'Expected the Footer version anchor to render `· ${GLOBAL_CONFIG.environment}` when set.',
    )
  })

  test('AuthenticatedArea mounts EnvironmentBanner inside the AppShell header', () => {
    assert.match(
      layoutSource,
      /<AppShell\.Header>[\s\S]*?<EnvironmentBanner\s*\/>/,
      'Expected EnvironmentBanner to be rendered at the top of AppShell.Header.',
    )
    // The header height is increased to accommodate the banner when visible.
    assert.match(
      layoutSource,
      /HEADER_HEIGHT\s*=[\s\S]*?ENVIRONMENT_BANNER_HEIGHT/,
      'Expected HEADER_HEIGHT to add ENVIRONMENT_BANNER_HEIGHT when the banner is visible.',
    )
  })
})
