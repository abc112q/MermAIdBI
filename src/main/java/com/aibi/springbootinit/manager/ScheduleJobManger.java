package com.aibi.springbootinit.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.aibi.springbootinit.common.BaseResponse;
import com.aibi.springbootinit.common.ResultUtils;
import com.aibi.springbootinit.constant.CommonConstant;
import com.aibi.springbootinit.model.dto.chart.ChartQueryRequest;
import com.aibi.springbootinit.model.entity.Chart;
import com.aibi.springbootinit.service.ChartService;
import com.aibi.springbootinit.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Ariel
 */
@Component
@Slf4j
// todo 这里实际上应该加入一些热点数据
public class ScheduleJobManger {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private ChartService chartService;


    @PostConstruct
    public void init() {
        // 在项目启动时进行缓存预热
        log.info("开始预热缓存");
        prejob();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleCacheUpdate() {
        // 每天凌晨3点更新缓存
        prejob();
    }

    public void prejob(){
        String cacheKey = "bichart" + "_" ;
        //Set keys = redisTemplate.keys(cacheKey + "*");
        //redisTemplate.delete(keys);
        List<Chart> allChartlist = chartService.list();
        for (Chart chart : allChartlist) {
            //如果key不存在添加到缓存
            if(!Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey+chart.getId()))) {
                redisTemplate.opsForList().rightPush(cacheKey+chart.getId(), JSONUtil.toJsonStr(chart));
                redisTemplate.expire(cacheKey+chart.getId(), 120 + RandomUtil.randomInt(), TimeUnit.MINUTES);
                log.info("将数据缓存到redis:"+chart.getId());
            }
        }

    }

}
