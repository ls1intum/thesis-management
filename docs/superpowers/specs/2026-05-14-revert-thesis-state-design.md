# Revert Thesis State

**Issue:** [#949](https://github.com/ls1intum/thesis-management/issues/949) — Bug: Can't revert student state from writing to proposal.

## Problem

The thesis lifecycle is forward-only: states (`PROPOSAL` → `WRITING` → `SUBMITTED` → `ASSESSED` → `GRADED` → `FINISHED`, plus the side branch `DROPPED_OUT`) are advanced via dedicated buttons (`acceptProposal`, `submitThesis`, `submitAssessment`, `gradeThesis`, `completeThesis`, `closeThesis`). Once advanced, there is no UI path to go back.

If a supervisor or examiner clicks the wrong button — or needs to reopen a `FINISHED` thesis because a required document was never uploaded — the only workaround is to edit the `changedAt` timestamps in the Configuration section, which does not change the current state. The reported bug is exactly this: changing the date of `WRITING` does not move the thesis out of `WRITING`.

## Goal

Let supervisors and examiners revert a thesis one state at a time. Repeated clicks can walk further back. Side data (proposal approval, assessment, final grade) is preserved so the supervisor can re-advance without re-entering values.

## UX

A new **"Revert to Previous State"** button in the Configuration section, next to the existing "Close Thesis" / "Update" buttons. Visible only when:

- `access.supervisor` is true (covers supervisors and examiners), AND
- `thesis.states.length >= 2` (i.e., the thesis is not still in its initial state).

Clicking it opens a confirmation dialog:

> Revert the thesis state from **[CurrentState]** back to **[PreviousState]**? Data captured in [CurrentState] (assessment, final grade, proposal approval, etc.) will be preserved.

On confirmation the request is sent; on success the thesis reloads and the `ThesisStateBadge` in the header reflects the previous state.

## Backend

### Endpoint

`POST /v2/theses/{thesisId}/revert-state`

- Auth: caller must have supervisor access on the thesis (`thesis.hasSupervisorAccess(currentUser)`), same check used by `acceptProposal`, `submitThesis`, etc.
- Returns the updated `ThesisDto`.

### Service method

`ThesisService.revertToPreviousState(Thesis thesis)`:

1. `requireNotAnonymized(thesis)`.
2. `assertCanAccessResearchGroup`.
3. Order `thesis.getStates()` by `changedAt DESC`. If fewer than two entries, throw `ResourceInvalidParametersException("Thesis has no previous state to revert to")`.
4. `currentChange` = first; `previousState` = second's state.
5. Capture `wasReopened = currentChange.state in (FINISHED, DROPPED_OUT)`.
6. Delete the `currentChange` row via `thesisStateChangeRepository.deleteById(currentChange.getId())` and remove it from the in-memory `thesis.getStates()` set.
7. `thesis.setState(previousState)`.
8. `thesis = thesisRepository.save(thesis)`.
9. If `wasReopened`: for each `student` in `thesis.getStudents()`, call `accessManagementService.addStudentGroup(student)` so the student regains application access. (Mirrors `closeThesis` / `completeThesis`, in reverse.)
10. Return the saved thesis.

### Data preservation

We do not clear:

- `proposal.approvedAt` / `approvedBy` when reverting from `WRITING` to `PROPOSAL`
- assessment entities when reverting from `ASSESSED` to `SUBMITTED`
- `finalGrade` / `finalFeedback` when reverting from `GRADED` to `ASSESSED`

This is intentional. The supervisor can re-advance and the existing values populate the forms. If they were destructive operations they would amplify the cost of a single misclick.

### No mailing

Reverting is an admin-correction action and does not send emails. The original forward-action email is already in the wild; we don't try to undo it.

## Frontend

### Request

```ts
const [reverting, onRevert] = useThesisUpdateAction(async () => {
  const response = await doRequest<IThesis>(
    `/v2/theses/${thesis.thesisId}/revert-state`,
    { method: 'POST', requiresAuth: true },
  )
  if (response.ok) return response.data
  throw new ApiError(response)
}, 'Thesis state reverted successfully')
```

### Button

In `ThesisConfigSection.tsx`, alongside "Close Thesis":

```tsx
{access.supervisor && (thesis.states?.length ?? 0) >= 2 && (
  <ConfirmationButton
    confirmationTitle='Revert Thesis State'
    confirmationText={`Revert from ${currentState} back to ${previousState}? Data captured in ${currentState} will be preserved.`}
    variant='outline'
    color='yellow'
    loading={reverting}
    onClick={onRevert}
  >
    Revert to Previous State
  </ConfirmationButton>
)}
```

The current/previous state labels come from the latest two entries in `thesis.states` sorted by `startedAt`.

## Edge cases

- **Anonymized thesis:** blocked server-side by `requireNotAnonymized`. The button can also be hidden when `thesis.anonymizedAt` is set, but the server check is the source of truth.
- **Initial state (single state change):** button hidden client-side; server returns 400 if called anyway.
- **Concurrent reverts:** relies on existing per-call Spring Data transactions.
- **Reverting a thesis whose student no longer exists:** `getStudents()` returns an empty list, group restoration is a no-op.

## Testing

### Server

- `ThesisServiceTest`:
  - Revert from `WRITING` → state becomes `PROPOSAL`, only one state change remains, proposal approval preserved.
  - Revert from `FINISHED` → state becomes `GRADED`, `addStudentGroup` invoked for each student.
  - Revert from `DROPPED_OUT` → state becomes prior state, `addStudentGroup` invoked.
  - Revert with only one state entry → throws `ResourceInvalidParametersException`.
  - Revert anonymized thesis → throws.
- `ThesisControllerTest` (or `ThesisControllerAdditionalTest`):
  - Supervisor: 200 + updated DTO.
  - Non-supervisor (student): 403.

### Client / e2e

Extend `client/e2e/thesis-lifecycle-workflow.spec.ts` (or add a small spec) to: accept proposal → assert state `WRITING` → click "Revert to Previous State" → confirm → assert state `PROPOSAL`.

## Out of scope

- Jumping multiple states in one action (still requires repeated clicks).
- Clearing forward-state data on revert.
- Reverting via the public API.
