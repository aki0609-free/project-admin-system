import { expect, test, type APIResponse, type Page } from '@playwright/test'

const password = 'playwright-local-only'

type Permission = { id: number; name: string }
type Role = { id: number; name: string }
type User = { id: number; username: string }

const responseJson = async <T>(response: APIResponse): Promise<T> => {
  const body = await response.text()
  expect(response.ok(), `${response.status()} ${response.url()}\n${body}`).toBeTruthy()
  return body ? JSON.parse(body) as T : {} as T
}

const authenticatedHeaders = async (page: Page) => {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'))
  expect(accessToken, 'authenticated access token').not.toBeNull()
  return {
    Authorization: `Bearer ${accessToken}`,
    'X-Tenant-ID': 'default',
  }
}

const ensureRole = async (
  page: Page,
  roleName: 'MANAGER' | 'OPERATOR',
  permissionNames: string[],
) => {
  const headers = await authenticatedHeaders(page)
  const permissions = await responseJson<Permission[]>(
    await page.request.get('/api/permissions', { headers }),
  )
  const permissionIds = permissions
    .filter(permission => permissionNames.includes(permission.name))
    .map(permission => permission.id)
  expect(permissionIds).toHaveLength(permissionNames.length)

  const roles = await responseJson<Role[]>(
    await page.request.get('/api/roles', { headers }),
  )
  const existing = roles.find(role => role.name === roleName)
  if (existing) {
    const response = await page.request.put(`/api/roles/${existing.id}`, {
      headers,
      data: { name: roleName, permissionIds },
    })
    expect(response.ok(), await response.text()).toBeTruthy()
    return
  }

  const response = await page.request.post('/api/roles', {
    headers,
    data: { name: roleName, permissionIds },
  })
  expect(response.ok(), await response.text()).toBeTruthy()
}

const ensureUser = async (
  page: Page,
  username: string,
  roleName: 'MANAGER' | 'OPERATOR',
) => {
  const headers = await authenticatedHeaders(page)
  const users = await responseJson<User[]>(
    await page.request.get('/api/users', { headers }),
  )
  const existing = users.find(user => user.username === username)
  const data = {
    username,
    password,
    enabled: true,
    roles: [roleName],
  }
  const response = existing
    ? await page.request.put(`/api/users/${existing.id}`, { headers, data })
    : await page.request.post('/api/users', { headers, data })
  expect(response.ok(), await response.text()).toBeTruthy()
}

const login = async (page: Page, username: string) => {
  await page.evaluate(() => localStorage.clear())
  await page.goto('/login')
  await page.getByLabel('ユーザー名').fill(username)
  await page.getByRole('textbox', { name: 'パスワード' }).fill(password)
  await page.getByRole('button', { name: 'ログイン' }).click()
  await expect(page).toHaveURL(/\/$/)
}

test('manager can view employees while operator is denied', async ({ page }) => {
  await page.goto('/')

  await ensureRole(page, 'MANAGER', [
    'dashboard:view',
    'customer:view',
    'master:view',
    'application:view',
    'employee:view',
    'operation:view',
  ])
  await ensureRole(page, 'OPERATOR', ['dashboard:view'])
  await ensureUser(page, 'playwright_manager', 'MANAGER')
  await ensureUser(page, 'playwright_operator', 'OPERATOR')

  await login(page, 'playwright_manager')
  await page.goto('/employee/information')
  await expect(page.getByRole('heading', { name: '従業員情報' })).toBeVisible()
  await page.goto('/system/rule')
  await expect(page).toHaveURL(/\/forbidden$/)
  await expect(page.getByText('アクセス権限がありません', { exact: true })).toBeVisible()

  await login(page, 'playwright_operator')
  await page.goto('/employee/information')
  await expect(page).toHaveURL(/\/forbidden$/)
  await expect(page.getByText('アクセス権限がありません', { exact: true })).toBeVisible()
})
