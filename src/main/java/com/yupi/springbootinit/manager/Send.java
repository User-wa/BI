package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.manager.WebSocketManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;

@Component
public class Send {

    @Resource
    private WebSocketManager webSocketManager;

//    @Scheduled(fixedDelay = 2000)
    public void sendMsg() throws IOException {
        webSocketManager.sendAllMessage("hello"+new Date());
    }
}