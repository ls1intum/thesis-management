import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #764.
//
// In ReplaceTopicModal the research-group Select was disabled and the
// dropdown was clipped to the current group whenever the user was not an
// admin, so a user who suggested a topic could not move it to another of
// their groups. The fix introduces an `isTopicOwner` flag (true when the
// current user is the topic's createdBy / a supervisor / an examiner) and
// allows owners to edit the field via `canEditResearchGroup`.

const here = dirname(fileURLToPath(import.meta.url))
const modalPath = resolve(
  here,
  '../src/pages/ManageTopicsPage/components/ReplaceTopicModal/ReplaceTopicModal.tsx',
)
const source = readFileSync(modalPath, 'utf8')

describe('ReplaceTopicModal — issue #764 (topic owner can change research group)', () => {
  test('useUser is imported alongside useHasGroupAccess', () => {
    assert.match(
      source,
      /import\s*\{[^}]*\buseUser\b[^}]*\}\s*from\s*['"][^'"]*hooks\/authentication['"]/,
      'Expected useUser to be imported from hooks/authentication so the modal can resolve the current user.',
    )
  })

  test('isTopicOwner is computed from createdBy / supervisors / examiners', () => {
    assert.match(
      source,
      /isTopicOwner\s*=/,
      'Expected an `isTopicOwner` variable derived from the topic and the current user.',
    )
    assert.match(
      source,
      /topic\.createdBy\?\.userId\s*===\s*currentUser\.userId/,
      'Expected isTopicOwner to consider topic.createdBy.',
    )
    assert.match(
      source,
      /topic\.supervisors\b[\s\S]*?\.userId\s*===\s*currentUser\.userId/,
      'Expected isTopicOwner to consider topic.supervisors.',
    )
    assert.match(
      source,
      /topic\.examiners\b[\s\S]*?\.userId\s*===\s*currentUser\.userId/,
      'Expected isTopicOwner to consider topic.examiners.',
    )
  })

  test('canEditResearchGroup combines admin access with ownership', () => {
    assert.match(
      source,
      /canEditResearchGroup\s*=\s*hasAdminAccess\s*\|\|\s*isTopicOwner/,
      'Expected `canEditResearchGroup = hasAdminAccess || isTopicOwner`.',
    )
  })

  test('the research group Select is disabled only when the user cannot edit', () => {
    assert.match(
      source,
      /disabled=\{[^}]*!canEditResearchGroup[^}]*\}/,
      'Expected the research-group Select to be `disabled` based on `!canEditResearchGroup`.',
    )
    // Regression guard: should no longer hard-pin the disabled clause to !hasAdminAccess
    assert.doesNotMatch(
      source,
      /disabled=\{[^}]*!hasAdminAccess\}/,
      'Expected the legacy `disabled={... !hasAdminAccess}` clause to be replaced by canEditResearchGroup.',
    )
  })

  test('the dropdown is only restricted to the current group when the user cannot edit', () => {
    assert.match(
      source,
      /if\s*\(\s*!canEditResearchGroup\s*&&\s*topic\?\.researchGroup\s*\)/,
      'Expected the "lock to current group" branch to use !canEditResearchGroup so owners receive the full list.',
    )
  })
})
