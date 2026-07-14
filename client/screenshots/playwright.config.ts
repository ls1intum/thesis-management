import { defineConfig, devices } from '@playwright/test'
import path from 'node:path'

/**
 * Playwright configuration for documentation screenshot capture.
 *
 * Kept in a dedicated file (not the main `playwright.config.ts`) so screenshot
 * jobs don't inherit test-run tweaks like retries or trace-on-retry, and so
 * a Playwright reporter change for e2e never accidentally reshuffles the
 * generated PNGs.
 */
export default defineConfig({
  testDir: __dirname,
  // The auth setup that produces .auth/*.json lives in ../e2e — we reuse it as
  // a "setup" project so a fresh checkout can capture screenshots in one call.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  expect: {
    timeout: 15_000,
  },
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'screenshot-report' }]],
  outputDir: path.join(__dirname, '..', '.screenshot-artifacts'),
  use: {
    baseURL: process.env.CLIENT_URL ?? 'http://localhost:3100',
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 2,
    // Freeze animations so hover / transition states don't jitter between runs.
    launchOptions: {
      args: ['--force-prefers-reduced-motion'],
    },
    // Screenshots must always succeed — we don't need Playwright to attach
    // its own diagnostic screenshot on failure.
    screenshot: 'off',
    trace: 'off',
    video: 'off',
  },
  projects: [
    {
      name: 'setup',
      // Reuse the same auth setup as the e2e suite. It writes to `e2e/.auth/`
      // relative to the working directory (`client/`), and the driver spec
      // reads from the same path via `authStatePath()`.
      testDir: path.join(__dirname, '..', 'e2e'),
      testMatch: /.*\.setup\.ts/,
    },
    {
      name: 'capture',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
      dependencies: ['setup'],
    },
  ],
})
