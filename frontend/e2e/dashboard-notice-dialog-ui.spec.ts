import { expect, test, type Page } from '@playwright/test'

type NoticeResponse = {
  id: number
}

const authenticatedHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()

  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

const currentBusinessDate = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Tokyo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date())

test('dashboard day and detail dialogs use the shared dialog layout', async ({ page }) => {
  const title = `E2E Dialog確認 ${Date.now()}`
  const date = currentBusinessDate()

  await page.goto('/')
  const headers = await authenticatedHeaders(page)
  const createResponse = await page.request.post('/api/notices', {
    headers,
    data: {
      title,
      start: date,
      end: date,
      type: 'INFO',
      color: 'blue',
      contentFormat: 'PLAIN_TEXT',
      content: '共通Dialogの本文表示確認',
      pinnedFlag: false,
      activeFlag: true,
    },
  })
  const createBody = await createResponse.text()
  expect(createResponse.ok(), createBody).toBeTruthy()
  const notice = JSON.parse(createBody) as NoticeResponse

  try {
    await page.reload()
    await page.getByText(title, { exact: true }).click()

    let detailDialog = page.getByRole('dialog').filter({
      has: page.getByRole('heading', { name: title, exact: true }),
    })
    await expect(detailDialog.getByText('共通Dialogの本文表示確認', { exact: true })).toBeVisible()
    await expect(detailDialog.getByRole('button', { name: '編集', exact: true })).toBeVisible()
    await expect(detailDialog.getByRole('button', { name: '削除', exact: true })).toBeVisible()
    await detailDialog.getByRole('button', { name: '閉じる', exact: true }).first().click()
    await expect(detailDialog).not.toBeVisible()

    await page.getByRole('button', { name: 'カレンダー', exact: true }).click()
    await page.getByText(title, { exact: true }).click()

    const dayDialog = page.getByRole('dialog').filter({
      has: page.getByRole('heading', { name: date, exact: true }),
    })
    await expect(dayDialog.getByText('1件', { exact: true })).toBeVisible()
    await expect(dayDialog.getByText(title, { exact: true })).toBeVisible()
    await dayDialog.getByText(title, { exact: true }).click()

    detailDialog = page.getByRole('dialog').filter({
      has: page.getByRole('heading', { name: title, exact: true }),
    })
    await expect(detailDialog.getByText('共通Dialogの本文表示確認', { exact: true })).toBeVisible()
    await detailDialog.getByRole('button', { name: '閉じる', exact: true }).first().click()
    await dayDialog.getByRole('button', { name: '閉じる', exact: true }).click()
  } finally {
    const deleteResponse = await page.request.delete(`/api/notices/${notice.id}`, { headers })
    expect(deleteResponse.ok(), await deleteResponse.text()).toBeTruthy()
  }
})
