import { describe, test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

// Regression test for issue #926.
//
// The "Add/Edit Final Grade" and "Mark thesis as finished" buttons in
// ThesisFinalGradeSection used to be gated by `access.supervisor`, but the
// server endpoints `/v2/theses/{id}/grade` and `/v2/theses/{id}/complete`
// require `hasExaminerAccess()`. A non-examiner supervisor saw the buttons,
// could fill the modal, and got AccessDeniedException on submit. The fix
// re-gated both buttons on `access.examiner`.
//
// We can't easily render the React component from a node:test (no JSX
// pipeline), and the e2e test fixtures don't include a stable thesis where
// a non-examiner supervisor in a matching research group can view a
// non-closed thesis. So this test pins the source: the two action-button
// gates must reference `access.examiner` and must NOT reference
// `access.supervisor`. Anyone reverting the fix will break this test.

const here = dirname(fileURLToPath(import.meta.url))
const sectionPath = resolve(
  here,
  '../src/pages/ThesisPage/components/ThesisFinalGradeSection/ThesisFinalGradeSection.tsx',
)
const source = readFileSync(sectionPath, 'utf8')

describe('ThesisFinalGradeSection — issue #926 (action buttons examiner-gated)', () => {
  test('the "Add/Edit Final Grade" button is gated by access.examiner', () => {
    // Match the JSX guard around the button that opens the SubmitFinalGradeModal.
    const re = /\{access\.examiner\s*&&\s*!isThesisClosed\(thesis\)\s*&&\s*\(/
    assert.match(
      source,
      re,
      'Expected the "Add/Edit Final Grade" button to be gated by `access.examiner && !isThesisClosed(thesis)`. ' +
        'See issue #926: gating on access.supervisor lets non-examiner supervisors open a modal whose submission the server rejects.',
    )
  })

  test('the "Mark thesis as finished" button is gated by access.examiner', () => {
    // Match the JSX guard around the onThesisComplete button.
    const re = /\{access\.examiner\s*&&\s*thesis\.state\s*===\s*ThesisState\.GRADED\s*&&\s*\(/
    assert.match(
      source,
      re,
      'Expected the "Mark thesis as finished" button to be gated by `access.examiner && thesis.state === ThesisState.GRADED`. ' +
        'See issue #926: the /complete endpoint also requires hasExaminerAccess on the server.',
    )
  })

  test('neither action-button gate references access.supervisor (regression guard)', () => {
    // The section-visibility guard is access.student; some other JSX may legitimately
    // reference access.supervisor, but the two action-button JSX gates must not.
    const supervisorActionGate =
      /\{access\.supervisor\s*&&\s*(?:!isThesisClosed\(thesis\)|thesis\.state\s*===\s*ThesisState\.GRADED)/
    assert.doesNotMatch(
      source,
      supervisorActionGate,
      'Found `access.supervisor` gating an action button — this is the issue #926 bug. ' +
        'Use `access.examiner` to match the server-side hasExaminerAccess() check.',
    )
  })
})
