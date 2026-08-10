import { expect, test } from '@playwright/test'

test('protected page redirects an unauthenticated user to login', async ({ page }) => {
  await page.goto('/operation/daily')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('button', { name: 'ログイン' })).toBeVisible()
})

test('invalid credentials display an error without leaving login', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('ユーザー名').fill('invalid-e2e-user')
  await page.getByRole('textbox', { name: 'パスワード' }).fill('invalid-e2e-password')
  await page.getByRole('button', { name: 'ログイン' }).click()

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByText('ログインに失敗しました')).toBeVisible()
})
