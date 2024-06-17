package com.aibi.springbootinit.bizmq;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.aibi.springbootinit.common.ErrorCode;
import com.aibi.springbootinit.exception.BusinessException;
import com.aibi.springbootinit.model.entity.Chart;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import com.aibi.springbootinit.manager.AIManager;
import com.aibi.springbootinit.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消费者，把生成图表的业务交给消费者处理
 */
@Component
@Slf4j
@SuppressWarnings("ALL")
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisTemplate redisTemplate;

    @RabbitListener( //设置监听的消息队列
            queues = {BiMqConstant.BI_QUEUE_NAME},
            ackMode ="MANUAL"
    )
    public void recieveMessage(String message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliverytag) throws IOException {
        //修改图表任务状态为”执行中“，等待执行成功后，修改为”已完成“，保存执行结果；执行失败后状态修改为”失败”，记录失败信息
        //消费者收到消息之后去执行业务逻辑
        try{
            if(StringUtils.isBlank(message)){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
            }

            long chartId= Long.parseLong(message);
            Chart chart=chartService.getById(chartId);
            if(chart == null){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图表为空");
            }
            Chart updateChart =new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            //修改表，将刚才注入的修改信息提交
            // int a=1/0;   在这里故意出错，发现重试次数没有生效，一直在无限重试，解决方法是队列加入ttl自动过期
            //后面加了死信队列，过期了就会进去
            boolean b=chartService.updateById(updateChart);
            if(!b){
                //todo 为什么异常抛出不会被程序感知
                HandleChartUpdateError(chart.getId(),"图表状态更改失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图表状态更改失败");
            }
            //调用AI
            String result=aiManager.doChat(buildUserInput(chart));
            String[] splits=result.split("####");
            if(splits.length<3){
                updateChart.setStatus("failed");
                chartService.updateById(updateChart);
                log.error("AI生成不符合要求");
                int a=1/0;
                //这里抛出的异常无法被捕获，是不是因为有事务,自己制造异常
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI生咸错误");

            }
            //去除生成内容的首尾空白字符
            String genChart=splits[1].trim();
            String genResult=splits[2].trim();

            Chart updateChartResult=new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGemResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult =chartService.updateById(updateChartResult);
            if(!updateResult){
                log.error("更新图表状态更改失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新图表状态更改失败");
            }
            // 执行成功，消息确认
            channel.basicAck(deliverytag,false);
            //将成功的结果插入到缓存,ttl加上随机数防止缓存雪崩，想了一下这里没必要加缓存，查询查不到的时候加进去就行
            redisTemplate.opsForValue().set("bichart"+"_" +updateChartResult.getId(),
                    JSONUtil.toJsonStr(updateChartResult),120+ RandomUtil.randomInt(), TimeUnit.MINUTES);
           // log.info("已存入到redis");
        }catch (Exception e){
            //第三个参数requeue = true：消息被否认后重新排队。
            //requeue = false：消息被否认后不重新排队，可能进入死信队列或被丢弃。
            //todo 这个重试可以改造为guava的,有空再说
            log.error("重试");
            channel.basicNack(deliverytag,false,true);
        }

    }

    /**
     * 构造用户输入
     * @param chart
     * @return
     */
    public String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析目标：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    private void HandleChartUpdateError(long chartID,String execMessage){
        Chart updateChart =new Chart();
        updateChart.setId(updateChart.getId());
        updateChart.setStatus("failed");
        updateChart.setExecMessage("execMessage");
        boolean updateResult =chartService.updateById(updateChart);
        if(!updateResult){
            log.error("图表失败信息更改失败"+chartID+","+execMessage);
        }
    }
}
