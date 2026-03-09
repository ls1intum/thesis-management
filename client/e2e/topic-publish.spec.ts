import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const publishableTopicTitle = 'E2E Gap6: Publishable Draft Topic'

test.describe('Topic Publish - Draft to Open', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can publish a draft topic via the edit modal', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/topics')
    await expect(page.getByRole('heading', { name: 'Manage Topics', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    const draftTab = page.getByText('Draft', { exact: true })
    const openTab = page.getByText('Open', { exact: true })

    await draftTab.click()

    // Find the publishable draft topic row
    const row = page.locator('tr').filter({ hasText: publishableTopicTitle }).first()
    const rowVisible = await row.isVisible({ timeout: 5_000 }).catch(() => false)

    if (!rowVisible) {
      // Topic may have already been published in a previous run — check Open tab
      await openTab.click()
      await expect(
        page.locator('tr').filter({ hasText: /E2E Gap6/ }).first(),
      ).toBeVisible({ timeout: 10_000 })
      return
    }

    // Click the edit button — first ActionIcon in the row actions
    const editButton = row.getByRole('button').first()
    await editButton.click()

    // Verify modal opens with correct title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })
    await expect(dialog.getByRole('heading', { name: 'Edit Topic', exact: true })).toBeVisible()

    // Verify it's a draft (should show both "Save Changes" and "Save & Create Topic" buttons)
    const publishButton = dialog.getByRole('button', { name: 'Save & Create Topic' })
    await expect(publishButton).toBeVisible()
    await expect(dialog.getByRole('button', { name: 'Save Changes' })).toBeVisible()

    // Click "Save & Create Topic" to publish the draft
    await expect(publishButton).toBeEnabled({ timeout: 5_000 })
    await publishButton.click()

    // Verify modal closes
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    // Verify the topic is no longer in the Draft tab
    await draftTab.click()
    await expect(
      page.locator('tr').filter({ hasText: publishableTopicTitle }),
    ).toBeHidden({ timeout: 5_000 })

    // Verify the topic now appears in the Open tab
    await openTab.click()
    await expect(
      page.locator('tr').filter({ hasText: /E2E Gap6/ }).first(),
    ).toBeVisible({ timeout: 10_000 })
  })
})
