package com.aibi.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列测试
 * 提交任务到线程池
 */
@RestController
@RequestMapping("/queue")
@Slf4j
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name) {
        CompletableFuture.runAsync(() -> {
            System.out.println("任务执行中：" + name+","+Thread.currentThread().getName()+"正在执行");
            try {
                Thread.sleep(60000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get(){
        Map<String,Object> map=new HashMap<>();
        int size=threadPoolExecutor.getQueue().size();
        map.put("队列长度",size);
        long taskCount=threadPoolExecutor.getTaskCount();
        map.put("任务总数",taskCount);
        long completedTaskCount=threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数",completedTaskCount);
        int activeCount =threadPoolExecutor.getActiveCount();
        map.put("正在执行任务的大致线程数",activeCount);
        return JSONUtil.toJsonStr(map);
    }

}
