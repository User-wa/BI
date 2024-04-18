package com.yupi.springbootinit.retry;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.utils.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GuavaRetryingTest {
    @Test
    void text(){
        GuavaRetrying guavaRetrying = new GuavaRetrying();
        guavaRetrying.retryDoChart(1L,"1");
    }

    @Test
    void text2(){
//        String html = "{\n" +
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
//        JSONObject jsonObject = JSONUtil.parseObj(html);
//        jsonObject.getStr("name");
//        String jsonString = "{\n" +
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
        String jsonString = "{\n" +
                "  \"tooltip\": {\n" +
                "    \"trigger\": \"item\"\n" +
                "  },\n" +
                "  \"legend\": {\n" +
                "    \"orient\": \"vertical\",\n" +
                "    \"left\": \"left\"\n" +
                "  },\n" +
                "  \"series\": [\n" +
                "    {\n" +
                "      \"name\": \"用户数\",\n" +
                "      \"type\": \"pie\",\n" +
                "      \"radius\": \"50%\",\n" +
                "      \"data\": [\n" +
                "        { \"value\": 10, \"name\": \"1号\" },\n" +
                "        { \"value\": 15, \"name\": \"2号\" },\n" +
                "        { \"value\": 20, \"name\": \"3号\" }\n" +
                "      ],\n" +
                "      \"emphasis\": {\n" +
                "        \"itemStyle\": {\n" +
                "          \"shadowBlur\": 10,\n" +
                "          \"shadowOffsetX\": 0,\n" +
                "          \"shadowColor\": \"rgba(0, 0, 0, 0.5)\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        boolean b = StringUtils.isValidStrictly(jsonString);
        System.out.println(b);

    }

    }