import { test, expect } from '@playwright/test'
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

test.describe('Abstract auto-extraction - Student', () => {
  test.use({ storageState: authStatePath('student') })

  test('uploading a thesis PDF extracts the abstract into the thesis', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, THESIS_1_URL)
    await hideWebpackOverlay(page)
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Upload the thesis document (writing phase) — mirrors thesis-file-upload.spec.ts.
    await expandAccordion(page, 'Thesis', page.getByText('Files').first())
    await expandAccordion(page, 'Files', page.getByRole('button', { name: 'Upload Thesis' }))

    const uploadThesisButton = page.getByRole('button', { name: 'Upload Thesis' })
    await uploadThesisButton.scrollIntoViewIfNeeded()
    await uploadThesisButton.click()

    const dialog = page.getByRole('dialog')
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

    // The extracted abstract is either auto-filled directly (blank abstract) or offered
    // as an editable suggestion (existing abstract). If a suggestion banner appears,
    // accept it so the assertion holds regardless of the thesis's seeded abstract.
    const useThisButton = page.getByRole('button', { name: 'Use this' })
    if (await useThisButton.isVisible({ timeout: 10_000 }).catch(() => false)) {
      await useThisButton.click()
    }

    // The abstract now shows the extracted text, with the line-end hyphenation rejoined
    // ("com-" + "prehensive" => "comprehensive").
    await expect(
      page.getByText(/comprehensive evaluation of automated review systems/i),
    ).toBeVisible({ timeout: 15_000 })
  })
})
