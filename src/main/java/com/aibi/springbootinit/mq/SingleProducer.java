package com.aibi.springbootinit.mq;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class SingleProducer{
    private final static String QUEUE_NAME="hello";

    public static void main(String[] args) {
        ConnectionFactory factory =new ConnectionFactory();
        factory.setHost("localhost");
       try(
               //建立连接和信道
           Connection connection=factory.newConnection();
           Channel channel =connection.createChannel()     //操作消息队列
       ){
           //创建消息队列
           channel.queueDeclare(QUEUE_NAME,false,false,false,null);
           String message="Hello world";
           //发送消息
           channel.basicPublish("", QUEUE_NAME,null,message.getBytes(StandardCharsets.UTF_8));
           System.out.println("Sent"+message+",");
       } catch (IOException | TimeoutException e) {
           e.printStackTrace();
       }
    }
}
