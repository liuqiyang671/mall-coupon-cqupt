package com.cqupt.settlement.handler;

import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

/**
 * 异步响应处理器，负责处理异步请求的超时、结果和异常。
 */
@Component
public class AsyncResponseHandler {

    /**
     * 创建一个 DeferredResult 对象，用于处理异步请求的结果。
     * <p>
     * 该方法会将 CompletableFuture 的结果包装成一个 DeferredResult 对象，并为其设置默认的超时和异常处理。
     *
     * @param future 包含异步处理结果的 CompletableFuture 对象
     * @param <T>    返回结果的类型
     * @return 一个封装了异步处理结果的 DeferredResult 对象
     */
    public <T> DeferredResult<Result<T>> createDeferredResult(CompletableFuture<T> future) {

        // 创建一个DeferredResult对象，设置超时时间为5000毫秒
        DeferredResult<Result<T>> deferredResult = new DeferredResult<>(5000L);

        future.thenAccept(result -> {
            // 设置处理成功的结果
            deferredResult.setResult(Results.success(result));
        }).exceptionally(ex -> {
            // 捕获异常，并设置错误结果
            deferredResult.setErrorResult(Results.failure());
            return null;
        });

        // 设置默认的超时响应
        deferredResult.onTimeout(() -> deferredResult.setErrorResult(Results.failure()));

        return deferredResult;
    }
}