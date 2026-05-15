export const testUsers = {
  merchant: {
    username: 'merchant01',
    password: 'Test123456',
    roleType: 1,
    expectedPath: /\/merchant(?:\/)?$/
  },
  customer: {
    username: 'customer01',
    password: 'Test123456',
    roleType: 2,
    expectedPath: /\/user\/products/
  }
} as const

export const seedGoods = {
  id: '800001',
  nameKeyword: 'CQUPT-GOODS-001'
} as const
