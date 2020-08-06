package com.guonl.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @SuppressWarnings("all")
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
            if ("YES".equals(isNullable)) {

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
        result.forEach(map -> {
            String tableName = (String) map.get("TABLE_NAME");
            String tableComment = (String) map.get("TABLE_COMMENT");
            // 1、添加表注释
            if (StringUtils.isBlank(tableComment)) {
                String alterSql = "alter table " + tableName + " comment " + "'注释：" + tableName + "'";
                log.info("添加表注释--->>>>>>>执行前的sql为：{}", alterSql);
                jdbcTemplate.update(alterSql);
            }
            // 2、检查表字段
            checkTableColumn(tableName, schemaName);

        });

        return ResponseEntity.ok(result);
    }


    @SuppressWarnings("all")
    private void checkTableColumn(String tableName, String schemaName) {
        List<String> list = Arrays.asList(tableName, schemaName);
        String sql = "select column_name,data_type,column_comment,is_nullable,column_type,column_default,column_key,extra" +
                " FROM INFORMATION_SCHEMA.Columns WHERE table_name=? AND table_schema=?";
        log.info("执行前的sql为：{}", sql);
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql, list.toArray());
        mapList.forEach(map -> {
            String columnName = (String) map.get("COLUMN_NAME");//字段名字
            String dataType = (String) map.get("DATA_TYPE");//数据类型
            dataType = dataType.toLowerCase();
            String comment = (String) map.get("COLUMN_COMMENT");//字段注释
            String isNullable = (String) map.get("IS_NULLABLE");//是否为非空，不建议把默认为null的字段设置为not null，如果之前存在null数据，设置的时候会报错
            String columnType = (String) map.get("COLUMN_TYPE");//字段类型 varchar(255)
            String columnDefault = (String) map.get("COLUMN_DEFAULT");//字段的默认值，如果为null，则设置
            String columnKey = (String) map.get("COLUMN_KEY");//主键  PRI
            String extra = (String) map.get("EXTRA");//主键描述 auto_increment 如果是自增长的，不需要更新
            //1、解决字段注释为空
            if (StringUtils.isBlank(comment)) {
                StringBuilder builder = new StringBuilder("alter table ");
                builder.append(tableName + " modify column ").append(columnName + " " + columnType).append(" comment " + "'注释：" + columnName + "'");
                log.info("添加字段注释--->>>>>>>执行前的sql为：{}", builder.toString());
                jdbcTemplate.update(builder.toString());
            }
            //2、解决默认值为空,需要跳过主键自增类型
//            alter table employee alter column double_test set default '0';
            if (StringUtils.isBlank(columnDefault) && !("PRI".equals(columnKey) && "auto_increment".equals(extra))) {
                StringBuilder builder = new StringBuilder("alter table ");
                builder.append(tableName + " alter column ").append(columnName).append(" set default ");
                if (dataType.contains("int")) {
                    builder.append("'0'");
                }
                if ("date".equals(dataType)) {
                    builder.append("'0000-00-00'");
                }
                if ("time".equals(dataType)) {
                    builder.append("'00:00:00'");
                }
                if ("timestamp".equals(dataType) || "datetime".equals(dataType)) {
                    builder.append("'0000-00-00 00:00:00'");
                }
                if ("double".equals(dataType) || "float".equals(dataType) || "decimal".equals(dataType)) {
                    builder.append("'0'");
                }
                if (dataType.contains("text")) {
                    builder.append("''");
                }
                if (dataType.contains("char")) {
                    builder.append("''");
                }
                //todo 其他数据类型待完善
                String defaultSql = builder.toString();
                if (defaultSql.contains("'")) {
                    log.info("添加默认值--->>>>>>>执行前的sql为：{}", builder.toString());
                    jdbcTemplate.update(builder.toString());
                }
            }
        });
        //3、检查是否缺少字段
        List<String> columnNameList = mapList.stream().map(x -> (String) x.get("COLUMN_NAME")).collect(Collectors.toList());
        //`is_delete` int(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
        if (!columnNameList.contains("is_delete")) {
            String addColumnSql = "alter table " + tableName + " add is_delete int(1) NOT NULL DEFAULT '0' COMMENT '是否删除'";
            log.info("添加系统字段--->>>>>>>执行前的sql为：{}", addColumnSql);
            jdbcTemplate.update(addColumnSql);
        }
        //sys_insert_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
        if (!columnNameList.contains("sys_insert_datetime")) {
            String addColumnSql = "alter table " + tableName + " add sys_insert_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'";
            log.info("添加系统字段--->>>>>>>执行前的sql为：{}", addColumnSql);
            jdbcTemplate.update(addColumnSql);
        }
        //sys_upd_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
        if (!columnNameList.contains("sys_upd_datetime")) {
            String addColumnSql = "alter table " + tableName + " add sys_upd_datetime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'";
            log.info("添加系统字段--->>>>>>>执行前的sql为：{}", addColumnSql);
            jdbcTemplate.update(addColumnSql);
        }

    }


}
