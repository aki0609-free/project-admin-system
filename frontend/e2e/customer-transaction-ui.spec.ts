import { expect, test } from '@playwright/test'

test('customer transaction shows payment states and calculates signed adjustment', async ({ page }) => {
  await page.goto('/customer/transaction')

  await expect(page.getByRole('heading', { name: '取引管理', exact: true })).toBeVisible()
  await expect(page.getByText('ローカル確認用：未入金', { exact: true })).toBeVisible()
  await expect(page.getByText('ローカル確認用：一部入金', { exact: true })).toBeVisible()
  await expect(page.getByText('ローカル確認用：その他調整額3円を含む', { exact: true }))
    .toBeVisible()

  await page.getByText('2026-06', { exact: true }).click()
  const dialog = page.getByRole('dialog')

  await expect(page.getByText('残高：129,450円', { exact: true })).toBeVisible()
  await page.getByRole('spinbutton', { name: 'その他調整額' }).fill('129450')
  await expect(page.getByText('回収額：330,000円', { exact: true })).toBeVisible()
  await expect(page.getByText('残高：0円', { exact: true })).toBeVisible()
  await expect(page.getByText('判定予定：入金済', { exact: true })).toBeVisible()

  const reason = page.getByRole('textbox', { name: '備考・調整理由' })
  await reason.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
  await reason.press('Backspace')
  await page.getByRole('button', { name: '入金確定' }).click()

  await expect(dialog).toBeVisible()
  await expect(page.getByText(
    'その他調整額を入力する場合は、備考へ調整理由を入力してください。',
    { exact: true },
  )).toBeVisible()

  await page.getByRole('button', { name: 'キャンセル' }).click()
})
