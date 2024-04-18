package com.yupi.springbootinit.retry;

import com.github.rholder.retry.*;
//import com.goblin.BIbackend.manager.AiManager;
//import com.goblin.BIbackend.utils.StringUtils;
import com.google.common.base.Predicates;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Author goblin
 * @Date 2024/4/8 21:07
 * @注释
 */
@Component
@Slf4j
public class GuavaRetrying {
    @Resource
    private AiManager aiManager;

    public String retryDoChart(Long modelId, String userInput){

        String aiResult = null;


        Callable<String> callable = () -> {
//            String jsonString = "{\n" +
//                "  \"xAxis\": {\n" +
//                "    \"data\": ['1号', '2号', '3号']\n" +
//                "  },\n" +
//                "  \"yAxis\": {},\n" +
//                "  \"series\": [\n" +
//                "    {\n" +
//                "      \"type\": 'bar',\n" +
//                "      \"stack\": \"总量\",\n" +
//                "      \"data\": [10, 15, 20]\n" +
//                "    }\n" +
//                "  ]\n" +
//                "}";
            // 业务逻辑
            return aiManager.doChat(modelId, userInput);
//            return jsonString;
        };

        // 定义重试器
        Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.<String>isNull()) // 如果结果为空则重试
                .retryIfResult(result ->{
                    String[] split = result.split("【【【【【");
                    if (split.length != 3){
                        // 格式不正确，重试
                        return true;
                    }
                    String getChart = split[1].trim();
                    if (StringUtils.isValidStrictly(getChart)){
//                    if (StringUtils.isValidStrictly(result)){
                        // 格式正确，不重试
                        return false;
                    }
                    return true;
                })
                .retryIfExceptionOfType(IOException.class) // 发生IO异常则重试
                .retryIfRuntimeException() // 发生运行时异常则重试
                // 初始等待时间，即第一次重试和第二次重试之间的等待时间为 3 秒。
                // 等待时间的增量，意味着每次重试后的等待时间会在前一次的基础上增加 2 秒。
                .withWaitStrategy(WaitStrategies.incrementingWait(3, TimeUnit.SECONDS, 2, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 允许执行3次（首次执行 + 最多重试2次）
                .withRetryListener(new MyRetryListener()) // 添加自定义的重试监听器
                .build();

        try {
            aiResult = retryer.call(callable);// 执行
        } catch (RetryException | ExecutionException e) { // 重试次数超过阈值或被强制中断
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成图像失败");
        }
        return aiResult;
    }
}
