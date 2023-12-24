package com.yupi.springbootinit.utils;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * excel相关工具类
 */
@Slf4j
public class ExcelUtils {

    public static String excelToCsv(MultipartFile multipartFile){
//        File file = null;
//        try {
//            file = ResourceUtils.getFile("classpath:数据.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new BusinessException(ErrorCode.OPERATION_ERROR,"文件传输异常");
//        }
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("csv转换错误");
            throw new RuntimeException(e);
        }
        StringBuilder stringBuilder = new StringBuilder();
        //读取表头
        if (CollUtil.isEmpty(list)){
            return null;
        }
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap)list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");
        //读取每一行数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> integerStringMap = (LinkedHashMap<Integer, String>)list.get(i);
            List<String> stringList = integerStringMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(stringList, ",")).append("\n");
        };
        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        excelToCsv(null);
    }

}
