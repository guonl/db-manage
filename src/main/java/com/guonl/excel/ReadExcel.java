package com.guonl.excel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReadExcel {

    private static List<TranslateVO> list = new ArrayList<>();

    /**
     * 读取excel表格中特定的列
     *
     * @param file  文件
     * @param index 第index列（0开始）
     * @throws Exception
     */
    public static void readColumn(File file, int index) throws Exception {
        InputStream inputStream = new FileInputStream(file.getAbsoluteFile());
        Workbook workbook = Workbook.getWorkbook(inputStream);
        Sheet sheet = workbook.getSheet(0);
        int rows = sheet.getRows();
        int columns = sheet.getColumns();
        for (int i = 1; i < rows; i++) {
            Cell cnCell = sheet.getCell(index, i);
            Cell enCell = sheet.getCell(index + 1, i);
            TranslateVO vo = new TranslateVO();
            String cnContents = cnCell.getContents();
            String enContents = enCell.getContents();
            if (StringUtils.isNoneBlank(cnContents)) {
                vo.setCnName(cnContents);
                vo.setEnName(enContents);
                list.add(vo);
            }
        }
    }

    private static List<TranslateVO> loadJson(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);

        try {
            // 将json文件的内容填充到Templare对象
            List<TranslateVO> list = JSON.parseObject(
                    inputStream,
                    Charset.defaultCharset(),
                    List.class
            );
            String s = JSON.toJSONString(list);
            return JSON.parseObject(s,new TypeReference<List<TranslateVO>>(){});
        } catch (IOException ex) {
            throw new RuntimeException("faile to parse json file");
        }
    }


//    public static void main(String[] args) {
//
//        File file = new File("C:\\Users\\guonl\\Desktop\\excel\\CRA混合版-修改.xls");
//        try {
//            readColumn(file, 0);//第一级
//            readColumn(file, 5);//第二级
//            readColumn(file, 8);//第三级
//            String s = JSON.toJSONString(list);
//            System.out.println(s);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    public static void main(String[] args) {
        List<TranslateVO> allList = new ArrayList<>();
        List<TranslateVO> personal = loadJson("personal.json");
        List<TranslateVO> group = loadJson("group.json");
        allList.addAll(personal);
        allList.addAll(group);
        Map<String, TranslateVO> collect = allList.stream().collect(Collectors.toMap(x -> x.getCnName(), x -> x, (k1, k2) -> k2));
        System.out.println(JSON.toJSONString(collect));
    }


}
