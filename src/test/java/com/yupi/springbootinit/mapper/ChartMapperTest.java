package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.service.ChartService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ChartMapperTest {

    @Resource
    private ChartMapper chartMapper;

    @Test
    void createTableById() {
//        long userId = 12344L;
//        String sql = "create table `chart`{ "+ userId +"}"+
//                "(\n" +
//                "    日期 varchar(128) null,\n" +
//                "    成员 varchar(128) null\n" +
//                ");";
        String sql = "create table `chart{123236}`\n" +
                "(\n" +
                "    日期 varchar(128) null,\n" +
                "    成员 varchar(128) null\n" +
                ")";
        chartMapper.createTableById(sql);
    }
}