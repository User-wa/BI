package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSocketManagerTest {
    @Resource
    private WebSocketManager webSocketManager;

    @Scheduled(fixedDelay = 2000)
    @Test
    void onClose() throws IOException {
        webSocketManager.sendAllMessage("hello"+new Date());
    }
}