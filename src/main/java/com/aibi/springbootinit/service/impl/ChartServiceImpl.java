package com.aibi.springbootinit.service.impl;


import com.aibi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aibi.springbootinit.mapper.ChartMapper;
import com.aibi.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
 * 图表服务实现
 * @author Ariel
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {


}


