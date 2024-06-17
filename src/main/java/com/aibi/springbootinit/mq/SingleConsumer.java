package com.aibi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

//单个消费者
public class SingleConsumer {
    private final static String QUEUE_NAME="hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory =new ConnectionFactory();
        factory.setHost("localhost");
                //建立连接和信道
        Connection connection=factory.newConnection();
        Channel channel =connection.createChannel() ;    //操作消息队列
            //创建消息队列
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        System.out.println("等待消息");
            //定义如何处理消息
        DeliverCallback deliverCallback =(consumerTag,delivery)->{
            String message=new String(delivery.getBody(),StandardCharsets.UTF_8);
        };
        //消费消息，会持续堵塞
        channel.basicConsume( QUEUE_NAME,true,deliverCallback,consumerTag ->{});
        System.out.println("接收完毕");
    }
}
