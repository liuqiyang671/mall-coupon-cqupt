package com.mall.cqupt.framework.idempotent;

/**
 * 幂等注解，防止用户重复提交表单信息
 */
public @interface NoRepeatSubmit {

    /**
     * 触发幂等失败逻辑时，返回的错误提示信息
     */
    String message() default "您操作太快，请稍后再试";
}
