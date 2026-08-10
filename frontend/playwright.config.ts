import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'
const target = new URL(baseURL)
const localHosts = new Set(['127.0.0.1', 'localhost', '::1'])

if (!localHosts.has(target.hostname) && process.env.E2E_ALLOW_REMOTE !== 'true') {
  throw new Error(
    `Remote E2E target is blocked: ${target.origin}. `
      + 'Set E2E_ALLOW_REMOTE=true only for an approved non-production environment.',
  )
}

export default defineConfig({
  testDir: './e2e',
  outputDir: './test-results/e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  use: {
    baseURL,
    locale: 'ja-JP',
    timezoneId: 'Asia/Tokyo',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'authentication',
      testMatch: /auth\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chromium',
      testIgnore: [/auth\.spec\.ts/, /.*\.setup\.ts/],
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.playwright/auth.json',
      },
    },
  ],
})
