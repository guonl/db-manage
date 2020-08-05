package com.guonl.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/db")
public class DbController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/getTableInfo")
    public ResponseEntity getTableInfo() {
        List<String> list = Arrays.asList("employee", "guonl");
        String sql = "select column_name,data_type,column_comment,is_nullable FROM INFORMATION_SCHEMA.Columns WHERE table_name=? AND table_schema=?";
        log.info("执行前的sql为：{}", sql);
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, list.toArray());
        return ResponseEntity.ok(maps);
    }


    @GetMapping("/column/adjust")
    public ResponseEntity columnAdjust(String tablename) {
        List<String> list = Arrays.asList(tablename);
        String sql = "select column_name,data_type,column_comment,is_nullable,column_type FROM INFORMATION_SCHEMA.Columns WHERE table_name=?";
        log.info("执行前的sql为：{}", sql);
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, list.toArray());
        result.forEach(map -> {
            String columnName = (String) map.get("COLUMN_NAME");
            String dataType = (String) map.get("DATA_TYPE");
            String comment = (String) map.get("COLUMN_COMMENT");
            String isNullable = (String) map.get("IS_NULLABLE");
            String columnType = (String) map.get("COLUMN_TYPE");
            //解决字段注释为空
            if (StringUtils.isBlank(comment)) {
                StringBuilder builder = new StringBuilder("alter table ");
                builder.append(tablename + " modify column ").append(columnName + " " + columnType).append(" comment " + "'注释：" + columnName + "'");
                log.info("添加字段注释--->>>>>>>执行前的sql为：{}", builder.toString());
                jdbcTemplate.update(builder.toString());
            }
            //解决默认值为空
            if("YES".equals(isNullable)){

            }
            //

        });
        return ResponseEntity.ok(result);
    }


    @GetMapping("/schema/adjust")
    public ResponseEntity schemaAdjust(String schemaName) {
         List list = Arrays.asList(schemaName);
        String sql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES WHERE table_schema=?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, list.toArray());
        result.forEach(map->{
            String tableName = (String) map.get("TABLE_NAME");
            String tableComment = (String) map.get("TABLE_COMMENT");
            // 1、添加表注释
            if(StringUtils.isBlank(tableComment)){
                String alterSql = "alter table " + tableName + " comment " + "'注释：" + tableName + "'";
                log.info("添加表注释--->>>>>>>执行前的sql为：{}", alterSql);
                jdbcTemplate.update(alterSql);
            }
            // 2、检查表字段注释
            checkTableColumn(tableName);

        });

        return ResponseEntity.ok(result);
    }

    private void checkTableColumn(String tableName) {
        List<String> list = Arrays.asList(tableName);
        String sql = "select column_name,data_type,column_comment,is_nullable,column_type FROM INFORMATION_SCHEMA.Columns WHERE table_name=?";
        log.info("执行前的sql为：{}", sql);
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, list.toArray());
        result.forEach(map -> {
            String columnName = (String) map.get("COLUMN_NAME");
            String dataType = (String) map.get("DATA_TYPE");
            String comment = (String) map.get("COLUMN_COMMENT");
            String isNullable = (String) map.get("IS_NULLABLE");
            String columnType = (String) map.get("COLUMN_TYPE");
            //1、解决字段注释为空
            if (StringUtils.isBlank(comment)) {
                StringBuilder builder = new StringBuilder("alter table ");
                builder.append(tableName + " modify column ").append(columnName + " " + columnType).append(" comment " + "'注释：" + columnName + "'");
                log.info("添加字段注释--->>>>>>>执行前的sql为：{}", builder.toString());
                jdbcTemplate.update(builder.toString());
            }
            //2、解决默认值为空
            if("YES".equals(isNullable)){
//                alter table employee modify name varchar(255) default '' not null;
//                alter table employee modify age int default '0' not null;


            }


        });
        //3、检查是否缺少字段
        List<String> columnNameList = result.stream().map(x -> (String) x.get("COLUMN_NAME")).collect(Collectors.toList());
        if(!columnNameList.contains("sys_create_time")){
            String addColumnSql = "alter table " + tableName + " add sys_create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'";
        }

    }


}
