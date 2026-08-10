import { expect, test as setup } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { dirname } from 'node:path'
import { ensureBusinessFixture } from './support/business-fixture'

const authFile = '.playwright/auth.json'

setup('authenticate local SYS_ADMIN', async ({ page }) => {
  const username = process.env.E2E_USERNAME ?? 'playwright_local'
  const password = process.env.E2E_PASSWORD ?? 'playwright-local-only'

  await page.goto('/login')
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByRole('textbox', { name: 'パスワード' }).fill(password)
  const loginResponsePromise = page.waitForResponse(
    response => response.url().endsWith('/auth/login'),
  )
  await page.getByRole('button', { name: 'ログイン' }).click()
  const loginResponse = await loginResponsePromise

  expect(loginResponse.status(), await loginResponse.text()).toBe(200)
  await expect(page).toHaveURL(/\/$/, { timeout: 15_000 })
  await expect(page.getByRole('button', { name: username })).toBeVisible()

  await ensureBusinessFixture(page)

  await mkdir(dirname(authFile), { recursive: true })
  await page.context().storageState({ path: authFile })
})
