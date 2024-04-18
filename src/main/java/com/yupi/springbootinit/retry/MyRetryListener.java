package com.yupi.springbootinit.retry;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author goblin
 * @Date 2024/4/8 22:20
 * @注释
 */
@Slf4j
public class MyRetryListener implements RetryListener {
    @Override
    public <V> void onRetry(Attempt<V> attempt) {
        log.info("重试次数: " + attempt.getAttemptNumber());
        log.info("延迟时间: " + attempt.getDelaySinceFirstAttempt() + " 毫秒");
        if (attempt.hasException()) {
            log.info("异常信息: " + attempt.getExceptionCause().getMessage());
        }
        if (attempt.hasResult()) {
            log.info("重试结果: " + attempt.getResult());
        }
    }
}
