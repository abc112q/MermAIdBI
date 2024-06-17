package com.aibi.springbootinit.mapper;

import com.aibi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.yupi.springbootinit.model.entity.Chart
 */
@Mapper
public interface ChartMapper extends BaseMapper<Chart> {

    //返回多条数据所以用List
    //这个报错无所谓
    List<Map<String, Object>> queryChartData(String querySql);
}
