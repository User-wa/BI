package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Test
    void doLimit() throws InterruptedException {
        Long userId = 1L;
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doLimit(String.valueOf(userId));
            System.out.println("ok");
        }
        Thread.sleep(1000);
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doLimit(String.valueOf(userId));
            System.out.println("ok");
        }

    }
}