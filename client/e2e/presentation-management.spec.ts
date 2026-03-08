import { test, expect } from '@playwright/test'
import { authStatePath, getAccordionItem, navigateTo } from './helpers'

const THESIS_ID = '00000000-0000-4000-d000-000000000018'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'E2E Gap9: Presentation Management Test Thesis'
const ORIGINAL_PRESENTATION_LOCATION = 'Room 01.07.023, Boltzmannstr. 3'
const UPDATED_PRESENTATION_LOCATION = 'Room 02.09.001, Updated Location'

/**
 * Expand the Presentation accordion on the thesis page.
 * Uses a precise label selector to avoid matching other accordion items
 * that may contain "Presentation" text in their content.
 */
async function expandPresentationAccordion(page: import('@playwright/test').Page) {
  const item = getAccordionItem(page, 'Presentation')
  const isExpanded = await item.evaluate((el) => el.hasAttribute('data-active')).catch(() => false)
  if (!isExpanded) {
    await item.locator('.mantine-Accordion-control').click()
  }

  return item
}

function getPresentationCard(section: import('@playwright/test').Locator, locationText: string) {
  const escapedLocationText = locationText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return section
    .getByText(new RegExp(escapedLocationText))
    .first()
    .locator('xpath=ancestor::div[contains(@class, "mantine-Card-root")][1]')
}

test.describe('Presentation Management - Edit', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can edit a presentation', async ({ page }) => {
    test.setTimeout(90_000)

    let cleanupRequired = false

    try {
      await navigateTo(page, THESIS_URL)
      await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
        timeout: 15_000,
      })

      // Expand the Presentation accordion
      const presentationSection = await expandPresentationAccordion(page)
      await expect(page.getByText('01.07.023').first()).toBeVisible({ timeout: 10_000 })

      // Verify both presentations are visible
      await expect(page.getByText('01.07.023').first()).toBeVisible()
      await expect(page.getByText('00.08.038').first()).toBeVisible()

      // Verify presentation type badges
      await expect(page.getByText('Intermediate').first()).toBeVisible()

      // Verify presentation state badges
      await expect(page.getByText('Scheduled').first()).toBeVisible()

      // Find the first presentation card (Room 01.07.023)
      const card = getPresentationCard(presentationSection, ORIGINAL_PRESENTATION_LOCATION)
      await expect(card).toBeVisible()

      // Open three-dot menu
      const menuButton = card.locator('button[aria-haspopup="menu"]')
      await expect(menuButton).toBeVisible()
      await menuButton.click()

      // Verify menu items are present
      await expect(
        page.getByRole('menuitem', { name: 'Edit Presentation', exact: true }),
      ).toBeVisible()

      // Click "Edit Presentation"
      await page.getByRole('menuitem', { name: 'Edit Presentation', exact: true }).click()

      // Verify the edit modal opens with correct title
      const modal = page.locator('section[role="dialog"]').filter({
        has: page.locator('.mantine-Modal-title').filter({ hasText: /^Update Presentation$/ }),
      })
      await expect(modal).toBeVisible({ timeout: 5_000 })
      await expect(
        modal.getByRole('heading', { name: 'Update Presentation', exact: true }),
      ).toBeVisible()

      // Verify form fields are present
      await expect(modal.getByLabel('Location')).toBeVisible()

      // Change the location
      const locationInput = modal.getByLabel('Location')
      await locationInput.clear()
      await locationInput.fill(UPDATED_PRESENTATION_LOCATION)

      // Submit
      await modal.getByRole('button', { name: 'Update Presentation' }).click()

      // Verify modal closes
      await expect(modal).not.toBeVisible({ timeout: 15_000 })

      // Verify success notification with exact text
      await expect(page.getByText('Presentation successfully updated')).toBeVisible({
        timeout: 10_000,
      })

      cleanupRequired = true

      // Verify the updated location is now displayed
      await expect(page.getByText('Updated Location').first()).toBeVisible({ timeout: 10_000 })
    } finally {
      if (cleanupRequired) {
        const presentationSection = await expandPresentationAccordion(page)
        const updatedCard = getPresentationCard(presentationSection, UPDATED_PRESENTATION_LOCATION)
        await expect(updatedCard).toBeVisible({ timeout: 10_000 })

        await updatedCard.locator('button[aria-haspopup="menu"]').click()
        await page.getByRole('menuitem', { name: 'Edit Presentation', exact: true }).click()

        const modal = page.locator('section[role="dialog"]').filter({
          has: page.locator('.mantine-Modal-title').filter({ hasText: /^Update Presentation$/ }),
        })
        await expect(modal).toBeVisible({ timeout: 5_000 })

        const locationInput = modal.getByLabel('Location')
        await locationInput.clear()
        await locationInput.fill(ORIGINAL_PRESENTATION_LOCATION)
        await modal.getByRole('button', { name: 'Update Presentation' }).click()

        await expect(modal).not.toBeVisible({ timeout: 15_000 })
        await expect(page.getByText('01.07.023').first()).toBeVisible({ timeout: 10_000 })
      }
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
    const presentationSection = await expandPresentationAccordion(page)

    // Guard: if the presentation was already deleted on a previous attempt, verify and return
    const presentationVisible = await page
      .getByText('00.08.038')
      .first()
      .isVisible({ timeout: 5_000 })
      .catch(() => false)
    if (!presentationVisible) {
      await expect(page.getByText('00.08.038')).toBeHidden()
      return
    }

    // Find the second presentation card (Room 00.08.038)
    const card = getPresentationCard(presentationSection, 'Room 00.08.038, Boltzmannstr. 3')
    await expect(card).toBeVisible()

    // Open three-dot menu
    const menuButton = card.locator('button[aria-haspopup="menu"]')
    await expect(menuButton).toBeVisible()
    await menuButton.click()

    // Click "Delete Presentation"
    await page.getByRole('menuitem', { name: 'Delete Presentation', exact: true }).click()

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
      const presentationSection = await expandPresentationAccordion(page)
      await expect(
        presentationSection.getByRole('heading', { name: 'Master Thesis Presentation' }).first(),
      ).toBeVisible({ timeout: 10_000 })

      // The edit workflow may temporarily change the first card's location.
      const card = presentationSection
        .locator('.mantine-Card-root')
        .filter({
          has: page.getByRole('heading', { name: 'Master Thesis Presentation', exact: true }),
        })
        .first()
      await expect(card).toBeVisible()

      const noteText = card.getByText('key discussion points').first()
      const addNoteButton = card.getByRole('button', { name: 'Add presentation note' })
      const showNoteButton = card.getByRole('button', { name: 'Show' })
      const editNoteButton = card.getByRole('button', { name: 'Edit' })

      // Retries may re-enter after the note was already saved. Re-open it if needed.
      if (
        !(await addNoteButton.isVisible({ timeout: 3_000 }).catch(() => false)) &&
        (await showNoteButton.isVisible({ timeout: 3_000 }).catch(() => false))
      ) {
        await showNoteButton.click()

        if (await noteText.isVisible({ timeout: 3_000 }).catch(() => false)) {
          await expect(noteText).toBeVisible()
          return
        }
      }

      if (await addNoteButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await addNoteButton.click()
      } else if (await editNoteButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
        await editNoteButton.click()
      }

      // Adding a new note opens the editor directly in edit mode
      const editor = card.locator('.ProseMirror[contenteditable="true"]').first()
      await expect(editor).toBeVisible({ timeout: 5_000 })

      // Verify Save and Cancel buttons appear in edit mode
      await expect(card.getByRole('button', { name: 'Save' })).toBeVisible()
      await expect(card.getByRole('button', { name: 'Cancel' })).toBeVisible()

      // Type the note in the ProseMirror editor
      await editor.click()
      const modifier = process.platform === 'darwin' ? 'Meta' : 'Control'
      await page.keyboard.press(`${modifier}+a`)
      await page.keyboard.type('E2E test presentation note: key discussion points')

      // Save
      await card.getByRole('button', { name: 'Save' }).click()

      // Verify success notification with exact text
      await expect(page.getByText('Presentation note updated successfully')).toBeVisible({
        timeout: 10_000,
      })

      if (!(await noteText.isVisible({ timeout: 2_000 }).catch(() => false))) {
        const showButtonAfterSave = card.getByRole('button', { name: 'Show' })
        if (await showButtonAfterSave.isVisible({ timeout: 2_000 }).catch(() => false)) {
          await showButtonAfterSave.click()
        }
      }

      await expect(noteText).toBeVisible({ timeout: 10_000 })
    } finally {
      await context.close()
    }
  })
})
