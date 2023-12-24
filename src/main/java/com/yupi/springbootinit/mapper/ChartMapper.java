package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 86134
* @description 针对表【chart(图表信息)】的数据库操作Mapper
* @createDate 2023-12-15 12:32:42
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    Integer createTableById(@Param("createTableSql") String createTableSql);

}




