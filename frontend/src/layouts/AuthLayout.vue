<template>
  <main class="auth-page">
    <section class="auth-visual" aria-label="邮惠券商城认证介绍">
      <div class="brand-row">
        <div class="brand-mark">邮</div>
        <div>
          <p class="eyebrow">重庆邮电大学学生项目</p>
          <h1>邮惠券商城</h1>
        </div>
      </div>
      <div class="coupon-preview">
        <div class="coupon-preview__amount">AI</div>
        <div>
          <strong>实时券核 · 智能结算</strong>
          <span>优惠券、商品、任务与购物车统一进入数字化控制台</span>
        </div>
      </div>
      <p class="auth-visual__copy">
        面向平台、商家和用户的现代化优惠券商城系统，支持模板维护、批量发券、领券高峰处理、购物车和结算用券。
      </p>
    </section>

    <section class="auth-panel">
      <slot />
    </section>
  </main>
</template>
