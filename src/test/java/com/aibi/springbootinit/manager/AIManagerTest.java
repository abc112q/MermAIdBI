package com.aibi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class AIManagerTest {
    @Resource
    private AIManager aiManager;

    @Test
    void doChat() {
        String answer=aiManager.doChat("分析需求：\n"+
                "分析网络用户的增长情况\n"+
                "原始数据\n"+
                "日期，用户数\n"+
                "1号，10\n"+
                "2号，20\n"+
                "3号，20\n");
        System.out.println(answer);
    }
}