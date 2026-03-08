import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const THESIS_ID = '00000000-0000-4000-d000-000000000018'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'E2E Gap9: Presentation Management Test Thesis'

/**
 * Expand the Presentation accordion on the thesis page.
 * Uses a precise label selector to avoid matching other accordion items
 * that may contain "Presentation" text in their content.
 */
async function expandPresentationAccordion(page: import('@playwright/test').Page) {
  const item = page.locator('.mantine-Accordion-item').filter({
    has: page.locator('.mantine-Accordion-label', { hasText: /^Presentation$/ }),
  })
  const isExpanded = await item.evaluate((el) => el.hasAttribute('data-active')).catch(() => false)
  if (!isExpanded) {
    await item.locator('.mantine-Accordion-control').click()
  }
}

test.describe('Presentation Management - Edit', () => {
  test('student can edit a presentation', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student5') })
    const page = await context.newPage()

    try {
      test.setTimeout(90_000)

      await navigateTo(page, THESIS_URL)
      await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Expand the Presentation accordion
      await expandPresentationAccordion(page)
      await expect(page.getByText('01.07.023').first()).toBeVisible({ timeout: 10_000 })

      // Verify both presentations are visible
      await expect(page.getByText('01.07.023').first()).toBeVisible()
      await expect(page.getByText('00.08.038').first()).toBeVisible()

      // Verify presentation type badges
      await expect(page.getByText('Intermediate').first()).toBeVisible()

      // Verify presentation state badges
      await expect(page.getByText('Scheduled').first()).toBeVisible()

      // Find the first presentation card (Room 01.07.023)
      const card = page.locator('.mantine-Card-root').filter({ hasText: '01.07.023' }).first()
      await expect(card).toBeVisible()

      // Open three-dot menu
      const menuButton = card
        .locator('button')
        .filter({ has: page.locator('svg') })
        .last()
      await menuButton.click()

      // Verify menu items are present
      await expect(page.getByText('Edit Presentation')).toBeVisible()
      await expect(page.getByText('Delete Presentation')).toBeVisible()

      // Click "Edit Presentation"
      await page.getByText('Edit Presentation').click()

      // Verify the edit modal opens with correct title
      const modal = page.getByRole('dialog').first()
      await expect(modal).toBeVisible({ timeout: 5_000 })
      await expect(modal.getByText('Update Presentation')).toBeVisible()

      // Verify form fields are present
      await expect(modal.getByLabel('Location')).toBeVisible()

      // Change the location
      const locationInput = modal.getByLabel('Location')
      await locationInput.clear()
      await locationInput.fill('Room 02.09.001, Updated Location')

      // Submit
      await modal.getByRole('button', { name: 'Update Presentation' }).click()

      // Verify modal closes
      await expect(modal).not.toBeVisible({ timeout: 15_000 })

      // Verify success notification with exact text
      await expect(page.getByText('Presentation successfully updated')).toBeVisible({
        timeout: 10_000,
      })

      // Verify the updated location is now displayed
      await expect(page.getByText('Updated Location').first()).toBeVisible({ timeout: 10_000 })
    } finally {
      await context.close()
    }
  })
})

test.describe('Presentation Management - Delete', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can delete a presentation', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_URL)
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // Expand the Presentation accordion
    await expandPresentationAccordion(page)
    await expect(page.getByText('00.08.038').first()).toBeVisible({ timeout: 10_000 })

    // Find the second presentation card (Room 00.08.038)
    const card = page.locator('.mantine-Card-root').filter({ hasText: '00.08.038' }).first()
    await expect(card).toBeVisible()

    // Open three-dot menu
    const menuButton = card
      .locator('button')
      .filter({ has: page.locator('svg') })
      .last()
    await menuButton.click()

    // Click "Delete Presentation"
    await page.getByText('Delete Presentation').click()

    // Verify confirmation dialog with correct title and text
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByText('Confirm Deletion')).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByText(/delete this presentation/i)).toBeVisible()
    await expect(dialog.getByText(/cannot be undone/i)).toBeVisible()

    // Click "Delete" button
    await dialog.getByRole('button', { name: 'Delete' }).click()

    // Verify success notification with exact text
    await expect(page.getByText('Presentation deleted successfully')).toBeVisible({
      timeout: 10_000,
    })

    // Verify the deleted presentation card is no longer visible
    await expect(page.getByText('00.08.038')).toBeHidden({ timeout: 10_000 })
  })
})

test.describe('Presentation Management - Notes', () => {
  test('student can add presentation note', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student5') })
    const page = await context.newPage()

    try {
      test.setTimeout(90_000)

      await navigateTo(page, THESIS_URL)
      await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Expand the Presentation accordion
      await expandPresentationAccordion(page)
      await expect(page.getByText('01.07.023').first()).toBeVisible({ timeout: 10_000 })

      // Find the first presentation card
      const card = page.locator('.mantine-Card-root').filter({ hasText: '01.07.023' }).first()
      await expect(card).toBeVisible()

      // Click "Add presentation note" button to expand the note section
      await card.getByRole('button', { name: 'Add presentation note' }).click()

      // Verify the note section expands (ProseMirror editor visible in read-only mode)
      const editor = card.locator('.ProseMirror')
      await expect(editor).toBeVisible({ timeout: 5_000 })

      // Click "Edit" button to enter edit mode (note section starts in read-only mode)
      await card.getByRole('button', { name: 'Edit' }).click()

      // Verify Save and Cancel buttons appear in edit mode
      await expect(card.getByRole('button', { name: 'Save' })).toBeVisible()
      await expect(card.getByRole('button', { name: 'Cancel' })).toBeVisible()

      // Type the note in the ProseMirror editor
      await editor.click()
      await page.keyboard.type('E2E test presentation note: key discussion points')

      // Save
      await card.getByRole('button', { name: 'Save' }).click()

      // Verify success notification with exact text
      await expect(page.getByText('Presentation note updated successfully')).toBeVisible({
        timeout: 10_000,
      })

      // Verify the note text is now displayed
      await expect(page.getByText('key discussion points').first()).toBeVisible({
        timeout: 10_000,
      })
    } finally {
      await context.close()
    }
  })
})
