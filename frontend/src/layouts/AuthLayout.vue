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
        <div class="coupon-preview__amount">¥30</div>
        <div>
          <strong>满 100 可用</strong>
          <span>邮惠券专属券卡体验，后台式高效管理</span>
        </div>
      </div>
      <p class="auth-visual__copy">
        重庆邮电大学学生打造的优惠券管理平台，支持账号安全、券模板维护和批量发券任务。
      </p>
    </section>

    <section class="auth-panel">
      <slot />
    </section>
  </main>
</template>
