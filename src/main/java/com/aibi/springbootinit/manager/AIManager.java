package com.aibi.springbootinit.manager;


import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ariel
 */
@Component
@Slf4j
public class AIManager {
    @Resource
    private SparkClient sparkClient;


    public final static String template= "####\n"
            +"{\n" +
            "  \"title\": {\n" +
            "    \"text\": \"用户数量变化趋势情况\"\n" +
            "  },\n" +
            "  \"tooltip\": {\n" +
            "    \"trigger\": \"item\"\n" +
            "  },\n" +
            "  \"legend\": {\n" +
            "    \"data\": [\"用户数量\"]\n" +
            "  },\n" +
            "  \"radar\": {\n" +
            "    \"indicator\": [\n" +
            "      { \"name\": \"指标1\", \"max\": 5 },\n" +
            "      { \"name\": \"指标2\", \"max\": 5 },\n" +
            "      { \"name\": \"指标3\", \"max\": 5 },\n" +
            "      { \"name\": \"指标4\", \"max\": 5 },\n" +
            "      { \"name\": \"指标5\", \"max\": 5 }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"series\": [{\n" +
            "    \"name\": \"用户数量\",\n" +
            "    \"type\": \"radar\",\n" +
            "    \"data\": [\n" +
            "      {\n" +
            "        \"value\": [2, 3, 4, 3, 2],\n" +
            "        \"name\": \"用户数量\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }]\n" +
            "}"+ "####\n" +"数据分析结论";

    /**
     * 向讯飞 AI 发送请求
     *
     * @return
     */
        public String doChat(String message){
            // 设置认证信息
            // 消息列表，可以在此列表添加历史对话记录
            List<SparkMessage> messages = new ArrayList<>();
            messages.add(SparkMessage.systemContent("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                    "分析需求：\n" +
                    "{数据分析的需求或目标}\n" +
                    "原始数据：\n" +
                    "{csv格式的原始数据，用,作为分隔符}\n" +
                    "请根据这两部分内容，按照以下指定格式生成内容（此外不要输入任何多余的开头、结尾、注释）\n" +
                    "####\n" +
                    "{前端 Echarts V5 的 option 配置对象的json代码，合理的将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                    "####\n" +
                    "{明确的数据分析结论、越详细越好，不要生成多余的注释、空格}"+"以下是一个生成代码的有效示例，请按照这种标准生成\n"+template
                   ));
            messages.add(SparkMessage.userContent(message));
            // 构造请求
            SparkRequest sparkRequest = SparkRequest.builder()
                    // 消息列表
                    .messages(messages)
                    // 模型回答的tokens的最大长度,非必传，默认为2048。
                    .maxTokens(2048)
                    // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                    .temperature(0.4)
                    // 指定请求版本，默认使用最新3.5版本
                    .apiVersion(SparkApiVersion.V3_5)
                    .build();
            String result ="";
            String useToken = " ";
            try {
                // 同步调用
                SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
                SparkTextUsage textUsage = chatResponse.getTextUsage();
                result = chatResponse.getContent();
                useToken = "提问tokens：" + textUsage.getPromptTokens()
                        + "，回答tokens：" + textUsage.getCompletionTokens()
                        + "，总消耗tokens：" + textUsage.getTotalTokens();
                log.info(useToken);
            } catch (SparkException e) {
                log.error("Ai调用发生异常了：" + e.getMessage());
            }
            return result;

        }

}