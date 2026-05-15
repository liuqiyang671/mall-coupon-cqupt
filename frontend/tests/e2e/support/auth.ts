import { expect, type Page } from '@playwright/test'

export interface LoginUser {
  username: string
  password: string
  roleType: number
  expectedPath: RegExp
}

export async function loginAs(page: Page, user: LoginUser) {
  await page.goto('/login')
  await expect(page.getByTestId('login-form')).toBeVisible()

  await page.getByTestId(`login-role-${user.roleType}`).click()
  await page.getByTestId('login-username').fill(user.username)
  await page.getByTestId('login-password').fill(user.password)
  await page.getByTestId('login-submit').click()

  await expect(page).toHaveURL(user.expectedPath, { timeout: 15_000 })
}
