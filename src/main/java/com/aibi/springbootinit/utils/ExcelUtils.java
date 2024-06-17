package com.aibi.springbootinit.utils;

import cn.hutool.core.collection.CollUtil;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
 * 题词压缩，将excel转化为csv
 * 利用easyexcel库
 */
public class ExcelUtils {

    public static String excel2Csv(MultipartFile multipartFile) throws FileNotFoundException {
        //调用hutool工具读文件
       // File file= ResourceUtils.getFile("classpath:test_excel.xlsx");
        List<Map<Integer,String>> list= null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //读取数据后转化为csv，1.读取表头
        if(CollUtil.isEmpty(list)){
            return "";
        }
        StringBuilder stringBuilder=new StringBuilder();  //线程不安全，但是性能高
        //linkedHashMap读取的是连续的，普通的map是无序的
        LinkedHashMap<Integer,String> headerMap =(LinkedHashMap)list.get(0);
        //过滤，用我的test示例来看过滤前后没什么区别
        List<String> headList =headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        //join拼接字符串，打印表头（表字段）headerMap.values()
        //System.out.println(StringUtils.join(headList,","));
        stringBuilder.append(StringUtils.join(headList,",")).append("\n");
        for(int i=1;i<list.size();i++){
            LinkedHashMap<Integer,String> dataMap=(LinkedHashMap) list.get(i);
            List<String> dataList =dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
           // System.out.println(StringUtils.join(dataList,","));
            stringBuilder.append(StringUtils.join(dataList,",")).append("\n");
        }
        //System.out.println(list);
        return stringBuilder.toString();
    }

    public static void main(String[] args) throws FileNotFoundException {
        excel2Csv(null);
    }

}
