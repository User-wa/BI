package com.yupi.springbootinit.redis;
import java.util.Date;

import cn.hutool.json.JSONUtil;
import com.yupi.springbootinit.model.vo.LoginUserVO;
import com.yupi.springbootinit.model.vo.UserVO;
import com.yupi.springbootinit.utils.SerializeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
class S2Test {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void test() {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(123L);
        loginUserVO.setUserName("name");
        loginUserVO.setUserAvatar("1");
        loginUserVO.setUserProfile("1");
        loginUserVO.setUserRole("12");
        loginUserVO.setCreateTime(new Date());
        loginUserVO.setUpdateTime(new Date());
        String json = JSONUtil.toJsonStr(loginUserVO);
        stringRedisTemplate.opsForValue().set("hallo", json);
    }
}