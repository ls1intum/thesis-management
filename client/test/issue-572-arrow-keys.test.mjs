import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #572.
//
// The ApplicationsSidebar keydown handler navigated between applications on
// ArrowLeft/ArrowRight regardless of focus, which moved the selected applicant
// while the user was still typing in a comment textarea or any other input.
//
// The fix early-returns from the handler when the event target is editable
// (input, textarea, select, contentEditable) and when modifier keys are held.
// We can't easily render the React component in node:test, so this test pins
// the source: the relevant guards must be present in the handler.

const here = dirname(fileURLToPath(import.meta.url))
const sidebarPath = resolve(
  here,
  '../src/pages/ReviewApplicationPage/components/ApplicationsSidebar/ApplicationsSidebar.tsx',
)
const source = readFileSync(sidebarPath, 'utf8')

describe('ApplicationsSidebar — issue #572 (arrow-key guard)', () => {
  test('keydown handler skips editable targets', () => {
    // Must check at least HTMLInputElement and HTMLTextAreaElement on the event target
    assert.match(
      source,
      /target\s+instanceof\s+HTMLInputElement/,
      'Expected the keydown handler to early-return when target is an HTMLInputElement.',
    )
    assert.match(
      source,
      /target\s+instanceof\s+HTMLTextAreaElement/,
      'Expected the keydown handler to early-return when target is an HTMLTextAreaElement.',
    )
    assert.match(
      source,
      /isContentEditable/,
      'Expected the keydown handler to early-return when target is contentEditable.',
    )
  })

  test('keydown handler ignores modifier-key combinations', () => {
    assert.match(
      source,
      /metaKey/,
      'Expected the keydown handler to skip events with metaKey held (OS shortcuts).',
    )
    assert.match(
      source,
      /ctrlKey/,
      'Expected the keydown handler to skip events with ctrlKey held.',
    )
  })

  test('keydown listener is attached via addEventListener (not window.onkeydown)', () => {
    // The previous implementation overwrote window.onkeydown, clobbering any other listener.
    assert.match(
      source,
      /window\.addEventListener\(['"]keydown['"]/,
      'Expected the listener to be attached via addEventListener so it does not clobber other handlers.',
    )
    assert.match(
      source,
      /window\.removeEventListener\(['"]keydown['"]/,
      'Expected the listener to be removed via removeEventListener on cleanup.',
    )
    assert.doesNotMatch(
      source,
      /window\.onkeydown\s*=/,
      'Expected window.onkeydown assignment to be removed in favor of addEventListener.',
    )
  })
})
