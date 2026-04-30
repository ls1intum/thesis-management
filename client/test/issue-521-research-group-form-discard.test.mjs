import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #521.
//
// ResearchGroupForm previously only had a Submit button (already disabled
// when the form was pristine). The fix adds a "Discard changes" button
// next to it that resets the form to its initial values; both buttons
// are disabled when no changes are pending.

const here = dirname(fileURLToPath(import.meta.url))
const formPath = resolve(here, '../src/components/ResearchGroupForm/ResearchGroupForm.tsx')
const source = readFileSync(formPath, 'utf8')

describe('ResearchGroupForm — issue #521 (discard changes)', () => {
  test('Discard changes button exists', () => {
    assert.match(
      source,
      /Discard changes/,
      'Expected a button labeled "Discard changes" to be rendered next to Submit.',
    )
  })

  test('Discard button is disabled when there are no changes', () => {
    // Look for `disabled={!hasChanges}` somewhere in the JSX block that
    // contains the "Discard changes" label.
    const discardBlock = source.match(/<Button[\s\S]*?Discard changes[\s\S]*?<\/Button>/)
    assert.ok(
      discardBlock,
      'Could not locate the Discard changes button block in the source.',
    )
    assert.match(
      discardBlock[0],
      /disabled=\{\s*!hasChanges\s*\}/,
      'Expected the Discard button to be disabled when !hasChanges.',
    )
  })

  test('Discard restores the form to its initial values', () => {
    // The handler must call form.setValues with the initial form fields
    // and reset the head display label.
    assert.match(
      source,
      /form\.setValues\(\{[\s\S]*?initialFormValues/,
      'Expected the Discard handler to call form.setValues with the original initial values.',
    )
    assert.match(
      source,
      /setHeadDisplayLabel\(/,
      'Expected the Discard handler to also reset the head display label.',
    )
  })

  test('Submit button is still disabled while the form is pristine', () => {
    // The pre-existing pristine guard for Submit must remain in place.
    assert.match(
      source,
      /type=['"]submit['"][\s\S]*?disabled=\{\s*!form\.isValid\(\)\s*\|\|\s*!hasChanges\s*\}/,
      'Expected the Submit button to remain disabled when the form is invalid or pristine.',
    )
  })
})
