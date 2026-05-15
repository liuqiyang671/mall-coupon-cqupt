import { expect, test } from '@playwright/test'
import { seedGoods, testUsers } from './fixtures/users'
import { loginAs } from './support/auth'

test.describe('customer shopping journey', () => {
  test('login, browse goods, add to cart, review summary, and enter settlement', async ({ page }) => {
    await loginAs(page, testUsers.customer)

    await expect(page.getByTestId('product-grid')).toBeVisible()
    await expect(page.getByTestId(`product-card-${seedGoods.id}`)).toBeVisible({ timeout: 15_000 })

    await page.getByTestId(`add-to-cart-${seedGoods.id}`).click()

    await page.goto('/user/cart')
    await expect(page.getByTestId('cart-page')).toBeVisible()
    await expect(page.getByTestId(`cart-item-${seedGoods.id}`)).toBeVisible({ timeout: 15_000 })

    const checkoutButton = page.getByTestId('checkout-button')
    await expect(checkoutButton).toBeEnabled()
    await checkoutButton.click()

    await expect(page).toHaveURL(/\/user\/settlement.*from=cart/, { timeout: 15_000 })
    await expect(page.getByTestId('settlement-page')).toBeVisible()
    await expect(page.getByTestId('settlement-goods-number-0')).toHaveValue(seedGoods.id)
    await expect(page.getByTestId('available-coupon-table')).toBeVisible()
  })
})
