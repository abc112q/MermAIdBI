package com.aibi.springbootinit.config;

import com.aibi.springbootinit.bizmq.BiMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ariel
 */
@Configuration
public class RabbitMQConfig {

    //重试机制有问题，只能等到队列过期后进入死信队列。
    @Bean(name="exchange1")
    public TopicExchange exchange() {
        return new TopicExchange(BiMqConstant.BI_EXCHANGE_NAME,true,false);
    }

    @Bean(name="queue1")
    public Queue queue() {
        Map<String,Object> args =new HashMap<>();
        //我的异常不生效，先用过期时间兜底
        args.put("x-message-ttl",2000);
        args.put("x-dead-letter-exchange", "dlExchange");
        args.put("x-dead-letter-routing-key", "dlKey");
        //开启消息持久化
        return new Queue(BiMqConstant.BI_QUEUE_NAME,true,false,false,args);
    }

    @Bean(name="binding1")
    public Binding binding(@Qualifier("queue1") Queue queue,@Qualifier("exchange1") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(BiMqConstant.BI_ROUTING_KEY);
    }


    /**
     *  配置死信
     */
    @Bean(name="exchange2")
    public TopicExchange dlExchage(){
        return new TopicExchange("dlExchange",true,false);
    }

    @Bean(name="queue2")
    public Queue dlQueue(){
        return new Queue("dlQueue",true);
    }

    @Bean
    public Binding dlbinding(@Qualifier("queue2") Queue queue,@Qualifier("exchange2")TopicExchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with("dlKey");
    }
}
