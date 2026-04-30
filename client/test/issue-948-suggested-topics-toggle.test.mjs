import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #948.
//
// `includeSuggestedTopics` was implicitly derived from the topics filter
// (sent as 'false' whenever the user picked specific topics), silently
// hiding suggested-topic applications - and creating the dashboard
// count vs. visible-list mismatch.
//
// The fix:
//   - Adds `includeSuggestedTopics` to IApplicationsFilters (default true)
//   - Renders an explicit Switch in the filter UI
//   - Sends the user's value verbatim to the backend
//   - Treats it as a real dependency in the fetch effect

const here = dirname(fileURLToPath(import.meta.url))
const contextPath = resolve(here, '../src/providers/ApplicationsProvider/context.ts')
const contextSource = readFileSync(contextPath, 'utf8')

const providerPath = resolve(
  here,
  '../src/providers/ApplicationsProvider/ApplicationsProvider.tsx',
)
const providerSource = readFileSync(providerPath, 'utf8')

const filtersPath = resolve(here, '../src/components/ApplicationsFilters/ApplicationsFilters.tsx')
const filtersSource = readFileSync(filtersPath, 'utf8')

describe('Application visibility — issue #948 (suggested-topic toggle)', () => {
  test('IApplicationsFilters declares includeSuggestedTopics', () => {
    assert.match(
      contextSource,
      /includeSuggestedTopics\?\s*:\s*boolean/,
      'Expected `includeSuggestedTopics?: boolean` on IApplicationsFilters.',
    )
  })

  test('ApplicationsProvider initializes the filter to true by default', () => {
    assert.match(
      providerSource,
      /useState<IApplicationsFilters>\(\{[\s\S]*?includeSuggestedTopics:\s*true/,
      'Expected the default filter state to set includeSuggestedTopics: true.',
    )
  })

  test('ApplicationsProvider sends the toggle value verbatim (no implicit derivation)', () => {
    assert.match(
      providerSource,
      /includeSuggestedTopics:\s*adjustedFilters\.includeSuggestedTopics\s*===\s*false\s*\?\s*['"]false['"]\s*:\s*['"]true['"]/,
      'Expected the request to send false only when the user explicitly turned it off.',
    )
    // Regression guard: the legacy `topics.includes('NO_TOPIC') ? 'true' : 'false'` ladder must be gone.
    assert.doesNotMatch(
      providerSource,
      /adjustedFilters\.topics\.includes\(['"]NO_TOPIC['"]\)\s*\?\s*['"]true['"]\s*:\s*['"]false['"]/,
      'Expected the implicit NO_TOPIC-based derivation to be removed in favor of the explicit toggle.',
    )
  })

  test('ApplicationsProvider re-fetches when the toggle changes', () => {
    // Look for the dep array of the fetching useEffect and assert it
    // contains adjustedFilters.includeSuggestedTopics.
    const fetchDeps = providerSource.match(
      /\}\s*,\s*\[([^\]]*adjustedFilters\.types[^\]]*)\]\s*\)/,
    )
    assert.ok(fetchDeps, 'Could not locate the application fetch useEffect dep array.')
    assert.match(
      fetchDeps[1],
      /adjustedFilters\.includeSuggestedTopics/,
      'Expected the fetch dep array to include `adjustedFilters.includeSuggestedTopics`.',
    )
  })

  test('Filters UI renders a Switch for "Include suggested topics"', () => {
    assert.match(
      filtersSource,
      /<Switch[\s\S]*?label=['"]Include suggested topics['"][\s\S]*?checked=\{filters\.includeSuggestedTopics\s*!==\s*false\}/,
      'Expected an `Include suggested topics` Switch bound to filters.includeSuggestedTopics.',
    )
  })
})
