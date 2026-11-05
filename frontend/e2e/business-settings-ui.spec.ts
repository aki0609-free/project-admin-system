import { expect, test } from '@playwright/test'

test('business settings exposes migrated business masters as dynamic tabs', async ({ page }) => {
  await page.goto('/admin/business-settings')

  await expect(page.getByRole('heading', { name: '業務管理' })).toBeVisible()
  await expect(page.getByRole('tab', { name: '年末調整', exact: true })).toHaveCount(0)
  await expect(page.getByRole('tab', { name: '退職時設定', exact: true })).toHaveCount(0)

  const resignationTab = page.getByRole('tab', { name: '退職時TODO', exact: true })
  await expect(resignationTab).toHaveCount(1, { timeout: 15_000 })
  await resignationTab.click()
  await expect(page.getByRole('heading', { name: '退職時TODO', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '退職時TODOを追加', exact: true })).toBeVisible()

  const closingTab = page.getByRole('tab', { name: '締日設定', exact: true })
  await expect(closingTab).toHaveCount(1)
  await closingTab.click()
  await expect(page.getByRole('heading', { name: '給与締日・支払日設定' })).toBeVisible()
  await expect(page.getByRole('button', { name: '締日設定を追加' })).toBeVisible()

  const payrollPolicyTab = page.getByRole('tab', { name: '給与制度設定', exact: true })
  await expect(payrollPolicyTab).toHaveCount(1)
  await payrollPolicyTab.click()
  await expect(page.getByRole('heading', { name: '会社・期間別給与制度' })).toBeVisible()
  await expect(page.getByRole('button', { name: '給与制度設定を追加' })).toBeVisible()

  await expect(page.getByRole('tab', { name: '締め帳票', exact: true })).toHaveCount(0)
  await expect(page.getByRole('tab', { name: 'プレビュー帳票', exact: true })).toHaveCount(0)

  await expect(page.getByRole('tab', { name: '帳票バックアップ', exact: true })).toHaveCount(0)

  await expect(page.getByRole('tab', { name: 'その他設定', exact: true })).toHaveCount(0)
})
