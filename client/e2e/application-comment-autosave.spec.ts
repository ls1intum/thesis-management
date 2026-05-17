import { test, expect } from '@playwright/test'
import { authStatePath, navigateToDetail } from './helpers'

// Regression test for issue #754:
//   Before the fix, typing a comment and immediately clicking on another
//   applicant in the sidebar would silently drop the text — the 500ms
//   auto-save debounce hadn't fired and the form-reset on the application
//   switch wiped the unsaved value. The fix flushes any pending edit in
//   the application-switch cleanup; this test guards that behaviour.
//
// The seeded applications c000-…-0040 (Student2) and c000-…-0041 (Student3)
// are dedicated to this test (see seed_dev_test_data.sql §42) so we can
// rely on Student2/Student3 being the only NOT_ASSESSED sidebar rows for
// those students when the supervisor opens /applications.

const APPLICATION_A_ID = '00000000-0000-4000-c000-000000000040' // Student2
const APPLICATION_B_ID = '00000000-0000-4000-c000-000000000041' // Student3

const COMMENT_TEXT = `issue-754 ${Date.now()} ${Math.random().toString(36).slice(2, 8)}`

test.describe('Application comment auto-save — issue #754', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('persists a typed comment when the reviewer switches applicants before the debounce fires', async ({
    page,
  }) => {
    // 1. Open applicant A's review page directly so we know exactly which
    //    application's comment we're editing.
    const studentAHeading = page.getByRole('heading', { name: /Student2 User/i })
    await navigateToDetail(page, `/applications/${APPLICATION_A_ID}`, studentAHeading)

    const commentField = page.getByLabel('Comment')
    await expect(commentField).toBeVisible({ timeout: 15_000 })

    // Resolve applicant B's sidebar row up-front so step 3 below is a bare
    // click with no preceding waits — that's the timing window the bug
    // lives in. Student3 has only one NOT_ASSESSED application (the one
    // seeded in §42), so the regex matches exactly one row in the sidebar.
    const studentBSidebarItem = page.getByText(/^Student3 User$/).first()
    await expect(studentBSidebarItem).toBeVisible({ timeout: 15_000 })

    // Clear any leftover text from previous runs and reset the saved
    // baseline on the server so this test starts from a known state.
    await commentField.fill('')
    // Give the 500ms debounce a moment to commit the empty value.
    await page.waitForTimeout(900)

    // 2. Type a unique comment. Do NOT wait for the debounce — we want to
    //    switch applicants *before* the auto-save fires (the exact race
    //    that issue #754 reported).
    await commentField.fill(COMMENT_TEXT)

    // 3. Immediately click applicant B's sidebar row. No await/visible
    //    check between fill and click — the whole point is to switch
    //    inside the 500ms debounce window.
    await studentBSidebarItem.click()

    // Wait for the URL to flip to applicant B, then for the body to
    // render the new applicant's heading.
    await page.waitForURL(`**/applications/${APPLICATION_B_ID}`, { timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /Student3 User/i })).toBeVisible({
      timeout: 15_000,
    })

    // 4. Give the flush PUT a beat to land on the server.
    await page.waitForTimeout(1000)

    // 5. Click back on applicant A. Force a reload to bypass any client
    //    cache and prove the comment is in fact persisted server-side.
    await page.goto(`/applications/${APPLICATION_A_ID}`)
    await expect(studentAHeading).toBeVisible({ timeout: 15_000 })

    const reloadedCommentField = page.getByLabel('Comment')
    await expect(reloadedCommentField).toBeVisible({ timeout: 15_000 })
    await expect(reloadedCommentField).toHaveValue(COMMENT_TEXT, { timeout: 10_000 })

    // 6. Restore the seed state so re-runs (and any other suite that ever
    //    inspects this application) see a clean comment.
    await reloadedCommentField.fill('')
    await page.waitForTimeout(900)
  })
})
