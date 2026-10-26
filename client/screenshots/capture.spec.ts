import { test } from '@playwright/test'
import path from 'node:path'
import fs from 'node:fs'
import { authStatePath } from '../e2e/helpers'
import { scenes } from './scenes'
import type { SceneRole } from './types'

/**
 * Screenshot capture driver.
 *
 * Every entry in the `scenes` registry is turned into a single Playwright test
 * that navigates to the target UI state and writes a PNG into
 * `documentation/static/img/screenshots/`. Failures are reported per scene so a single
 * broken scene doesn't take down the whole batch.
 *
 * Group scenes by role so we can attach the right storageState via
 * `test.use()` — Playwright applies test.use per describe block, not per test.
 */

const OUTPUT_DIR = path.resolve(
  __dirname,
  '..',
  '..',
  'documentation',
  'static',
  'img',
  'screenshots',
)

fs.mkdirSync(OUTPUT_DIR, { recursive: true })

const ROLES: readonly SceneRole[] = [
  'anonymous',
  'student',
  'student2',
  'student3',
  'student4',
  'student5',
  'supervisor',
  'supervisor2',
  'examiner',
  'examiner2',
  'admin',
]

for (const role of ROLES) {
  const scenesForRole = scenes.filter((s) => s.role === role)
  if (scenesForRole.length === 0) continue

  test.describe(`Screenshots (${role})`, () => {
    // Anonymous scenes explicitly clear the shared storage state so the app
    // renders the public landing surface, not the authenticated dashboard.
    if (role === 'anonymous') {
      test.use({ storageState: { cookies: [], origins: [] } })
    } else {
      test.use({ storageState: authStatePath(role) })
    }

    for (const scene of scenesForRole) {
      test(`${scene.filename} — ${scene.description}`, async ({ page }) => {
        test.setTimeout(120_000)

        if (scene.viewport) {
          await page.setViewportSize(scene.viewport)
        }

        await scene.run(page)

        const outputPath = path.join(OUTPUT_DIR, `${scene.filename}.png`)
        await page.screenshot({
          path: outputPath,
          fullPage: scene.fullPage ?? false,
          clip: scene.fullPage ? undefined : scene.clip,
        })

        // Attach the file to the HTML report so reviewers can eyeball the
        // shots directly instead of hunting through the repo.
        await test.info().attach(scene.filename, {
          path: outputPath,
          contentType: 'image/png',
        })
      })
    }
  })
}
