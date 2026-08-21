import { expect, test } from '@playwright/test'

test('applicant management uses the shared dialog and validates required fields', async ({ page }) => {
  await page.route('**/api/applicants', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    })
  })

  await page.goto('/application/applicant')

  await expect(page.getByRole('heading', { name: '応募者管理' })).toBeVisible()
  await page.getByRole('button', { name: '新規登録', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: '応募者 新規登録' })).toBeVisible()

  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('氏名は必須です', { exact: true })).toBeVisible()

  await dialog.getByRole('button', { name: 'キャンセル', exact: true }).click()
  await expect(dialog).toBeHidden()
})

test('application media keeps pivot editing, aggregation, and operation dialogs', async ({ page }) => {
  await page.route('**/api/application-media', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 101,
          mediaName: 'Indeed',
          mediaArea: '東京',
          mediaSlots: 2,
          mediaYearMonth: '2026-08',
          cost: 100000,
          hires: 4,
          unitPrice: 25000,
        },
        {
          id: 102,
          mediaName: '求人ボックス',
          mediaArea: '大阪',
          mediaSlots: 1,
          mediaYearMonth: '2026-08',
          cost: 60000,
          hires: 2,
          unitPrice: 30000,
        },
      ]),
    })
  })

  await page.goto('/application/media')

  await expect(page.getByRole('heading', { name: '応募媒体管理' })).toBeVisible()
  await expect(page.getByText('2026年8月', { exact: true })).toBeVisible()
  await expect(page.getByText('Indeed', { exact: true })).toBeVisible()
  await expect(page.getByText('100,000円', { exact: true })).toBeVisible()
  await expect(page.getByText('160,000円', { exact: true })).toBeVisible()

  await page.getByText('東京', { exact: true }).click()
  const areaInput = page.locator('.mlh-editable-cell input').first()
  await areaInput.fill('横浜')
  await areaInput.press('Enter')
  await expect(page.getByText('横浜', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '保存', exact: true })).toBeEnabled()

  await page.getByRole('button', { name: '媒体追加', exact: true }).click()
  const addMediaDialog = page.getByRole('dialog')
  await expect(addMediaDialog.getByRole('heading', { name: '媒体追加' })).toBeVisible()
  await addMediaDialog.getByLabel('応募媒体名', { exact: true }).fill('E2E媒体')
  await addMediaDialog.getByRole('button', { name: '追加', exact: true }).click()
  await expect(addMediaDialog).toBeHidden()
  await expect(page.getByText('E2E媒体', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: '年月追加', exact: true }).click()
  const addYearMonthDialog = page.getByRole('dialog')
  await expect(addYearMonthDialog.getByRole('heading', { name: '年月追加' })).toBeVisible()
  await addYearMonthDialog.getByRole('button', { name: 'キャンセル', exact: true }).click()

  await page.getByRole('button', { name: 'チャート', exact: true }).click()
  await expect(page.getByText('月別コスト推移', { exact: true })).toBeVisible()
  await expect(page.locator('canvas').first()).toBeVisible()
})
