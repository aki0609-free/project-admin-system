import { expect, test, type Page } from '@playwright/test'

const authHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken).not.toBeNull()
  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

test('document management exposes editable and read-only areas from server policy', async ({
  page,
}) => {
  await page.goto('/admin/document')

  await expect(page.getByRole('heading', { name: '書類管理', exact: true })).toBeVisible()
  await expect(page.getByText('SYS_ADMIN専用', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /自由書類/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /生成帳票/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /バックアップ/ })).toBeVisible()
  await expect(page.getByRole('button', { name: /テンプレート/ })).toBeVisible()
  await expect(page.getByText('編集可能', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: /バックアップ/ }).click()
  await expect(page.getByText('参照専用', { exact: true })).toBeVisible()
})

test('document APIs enforce list and managed-area write boundaries', async ({ page }) => {
  await page.goto('/')
  const headers = await authHeaders(page)

  const areasResponse = await page.request.get('/api/admin/documents/areas', { headers })
  expect(areasResponse.ok(), await areasResponse.text()).toBeTruthy()
  const areas = (await areasResponse.json()) as Array<{
    area: string
    allowedOperations: string[]
  }>
  expect(areas.find((area) => area.area === 'GENERAL')?.allowedOperations).toContain('UPLOAD')
  expect(areas.find((area) => area.area === 'BACKUPS')?.allowedOperations).not.toContain('UPLOAD')

  const oversizedPageResponse = await page.request.get(
    '/api/admin/documents/GENERAL/entries?maxKeys=1001',
    { headers },
  )
  expect(oversizedPageResponse.status()).toBe(400)

  const writeResponse = await page.request.post('/api/admin/documents/BACKUPS/directories', {
    headers,
    data: { path: 'must-not-be-created' },
  })
  expect(writeResponse.status()).toBe(403)
})
