package com.xu.springbootinit.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface ChartDataMapper {
    // 创建数据表
    int createTable(@Param("tableName") String tableName, @Param("header") String[] header);

    // 插入数据
    boolean insertData(@Param("tableName") String tableName, @Param("data") String[] data);

    List<Object> selectAll(@Param("tableName") String tableName);
}
