import { expect, test } from '@playwright/test'
import { testUsers } from './fixtures/users'
import { loginAs } from './support/auth'

test.describe('authentication and role routing', () => {
  test('customer login redirects to product browsing', async ({ page }) => {
    await loginAs(page, testUsers.customer)

    await expect(page.getByTestId('product-grid')).toBeVisible()
    await expect(page).toHaveURL(/\/user\/products/)
  })

  test('merchant login redirects to merchant workspace', async ({ page }) => {
    await loginAs(page, testUsers.merchant)

    await expect(page).toHaveURL(/\/merchant(?:\/)?$/)
    await expect(page.locator('.app-shell')).toBeVisible()
  })

  test('customer cannot stay on merchant routes', async ({ page }) => {
    await loginAs(page, testUsers.customer)

    await page.goto('/merchant')

    await expect(page).toHaveURL(/\/user\/products/)
    await expect(page.getByTestId('product-grid')).toBeVisible()
  })
})
