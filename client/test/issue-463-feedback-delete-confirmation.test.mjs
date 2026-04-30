import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #463.
//
// The trash icon in ThesisFeedbackOverview used a plain Mantine Button that
// triggered deleteFeedback on click without confirmation, so a misclick
// permanently removed the entry. The fix wraps the action in the existing
// ConfirmationButton component, which opens a Cancel/Confirm modal.
//
// Source-pin the relevant import + JSX so a regression that reverts to a
// plain Button (and skips confirmation) breaks this test.

const here = dirname(fileURLToPath(import.meta.url))
const overviewPath = resolve(
  here,
  '../src/pages/ThesisPage/components/ThesisFeedbackOverview/ThesisFeedbackOverview.tsx',
)
const source = readFileSync(overviewPath, 'utf8')

describe('ThesisFeedbackOverview — issue #463 (delete confirmation)', () => {
  test('imports the shared ConfirmationButton component', () => {
    assert.match(
      source,
      /import\s+ConfirmationButton\s+from\s+['"][^'"]*ConfirmationButton\/ConfirmationButton['"]/,
      'Expected ConfirmationButton to be imported from components/ConfirmationButton/ConfirmationButton.',
    )
  })

  test('the trash button is rendered as ConfirmationButton', () => {
    // The full element opens with attributes that include confirmationTitle / confirmationText
    assert.match(
      source,
      /<ConfirmationButton[\s\S]*?confirmationTitle=['"]Delete feedback\?['"][\s\S]*?onClick=\{\(\)\s*=>\s*deleteFeedback\(item\)\}/,
      'Expected the trash icon to be rendered inside <ConfirmationButton confirmationTitle="Delete feedback?" ... onClick={() => deleteFeedback(item)}>.',
    )
  })

  test('a confirmation message is shown to the user', () => {
    assert.match(
      source,
      /confirmationText=['"][^'"]+['"]/,
      'Expected ConfirmationButton to receive a non-empty confirmationText prop describing the consequences.',
    )
  })

  test('plain Button is no longer wired directly to deleteFeedback', () => {
    // Guards against a regression that reverts to <Button onClick={() => deleteFeedback(item)}>
    assert.doesNotMatch(
      source,
      /<Button[^>]*onClick=\{\(\)\s*=>\s*deleteFeedback\(item\)\}/,
      'Expected the bare <Button onClick={() => deleteFeedback(item)}> to be removed in favor of ConfirmationButton.',
    )
  })
})
