import { test, expect, type Page } from '@playwright/test'
import {
  authStatePath,
  navigateTo,
  expandAccordion,
  hideWebpackOverlay,
  createAbstractTestPdfBuffer,
} from '../helpers'

// Seeded thesis in WRITING state owned by the "student" user (same thesis the
// thesis file upload test uses). Uploading a thesis document triggers the
// server-side abstract extraction.
const THESIS_1_ID = '00000000-0000-4000-d000-000000000001'
const THESIS_1_URL = `/theses/${THESIS_1_ID}`

const EXTRACTED = /comprehensive evaluation of automated review systems/i

async function uploadThesisPdf(page: Page) {
  await expandAccordion(page, 'Thesis', page.getByText('Files').first())
  await expandAccordion(page, 'Files', page.getByRole('button', { name: 'Upload Thesis' }))

  const uploadThesisButton = page.getByRole('button', { name: 'Upload Thesis' })
  await uploadThesisButton.scrollIntoViewIfNeeded()
  await uploadThesisButton.click()

  // Scope to the upload dialog by name: after upload the confirmation modal opens, so a bare
  // getByRole('dialog') would match two dialogs.
  const dialog = page.getByRole('dialog', { name: 'File Upload' })
  await expect(dialog).toBeVisible({ timeout: 10_000 })
  await dialog.locator('input[type="file"]').setInputFiles({
    name: 'thesis-with-abstract.pdf',
    mimeType: 'application/pdf',
    buffer: createAbstractTestPdfBuffer(),
  })

  const uploadFileButton = dialog.getByRole('button', { name: 'Upload File' })
  await expect(uploadFileButton).toBeEnabled({ timeout: 10_000 })
  await uploadFileButton.click()
  await expect(dialog).toBeHidden({ timeout: 10_000 })
}

test.describe('Abstract auto-extraction - Student', () => {
  test.use({ storageState: authStatePath('student') })

  test('auto-fills a blank abstract, then asks via a modal before replacing an existing one', async ({
    page,
  }) => {
    test.setTimeout(120_000)

    await navigateTo(page, THESIS_1_URL)
    await hideWebpackOverlay(page)
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // 1) The seeded abstract is blank, so a confident extraction fills it silently — no modal.
    //    The line-end hyphenation is rejoined ("com-" + "prehensive" => "comprehensive").
    await uploadThesisPdf(page)
    await expect(page.getByText(EXTRACTED)).toBeVisible({ timeout: 15_000 })

    // 2) Replace the abstract with a manual edit; a later upload must NOT overwrite it silently.
    await page.getByRole('button', { name: 'Edit' }).first().click()
    const abstractEditor = page
      .locator('.mantine-InputWrapper-root', { hasText: 'Abstract' })
      .locator('.ProseMirror')
      .first()
    await abstractEditor.click()
    await page.keyboard.press('ControlOrMeta+A')
    await page.keyboard.press('Backspace')
    await abstractEditor.pressSequentially('A manually written abstract that must not be overwritten.')
    await page.getByRole('button', { name: 'Save' }).click()
    await expect(page.getByText(/manually written abstract that must not be overwritten/i)).toBeVisible({
      timeout: 10_000,
    })

    // 3) Uploading again surfaces a confirmation modal instead of overwriting the manual abstract.
    await uploadThesisPdf(page)
    const modal = page.getByRole('dialog', { name: /use the abstract extracted/i })
    await expect(modal).toBeVisible({ timeout: 15_000 })

    // 4) Confirming replaces the abstract with the extracted text.
    await modal.getByRole('button', { name: 'Use extracted abstract' }).click()
    await expect(page.getByText(EXTRACTED)).toBeVisible({ timeout: 10_000 })
  })
})
