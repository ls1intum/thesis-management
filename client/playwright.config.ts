import { defineConfig, devices } from '@playwright/test'

// Application sessions are tied to the browser family and operating system of the User-Agent that created
// them; a session used with another one ends (USER_AGENT_MISMATCH). The login setup, the test project and the
// contexts that specs open themselves with `browser.newContext()` (which do not inherit project options) must
// therefore present the same User-Agent. `devices['Desktop Chrome']` would send a Windows one, which no
// `browser.newContext()` call gets, so keep the browser's own User-Agent everywhere.
const browserIdentity = { ...devices['Desktop Chrome'], userAgent: undefined }

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 2 : 8,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['github']] : [['html', { open: 'never' }]],
  timeout: 60_000,
  expect: {
    timeout: 15_000,
  },
  use: {
    baseURL: process.env.CLIENT_URL ?? 'http://localhost:3100',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'on-first-retry',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      use: { ...browserIdentity },
    },
    {
      name: 'chromium',
      use: {
        ...browserIdentity,
        storageState: 'e2e/.auth/student.json',
      },
      dependencies: ['setup'],
    },
  ],
  webServer: process.env.CI
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://localhost:3100',
        reuseExistingServer: true,
        timeout: 120_000,
      },
})
