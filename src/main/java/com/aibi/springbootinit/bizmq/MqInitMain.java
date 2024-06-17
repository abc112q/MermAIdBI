package com.aibi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MqInitMain {
    //最好在测试中就创建好消息队列
    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory=new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection =factory.newConnection();
        Channel channel =connection.createChannel();
        String EXCHANGE_NAME="code_exchange";
        channel.exchangeDeclare(EXCHANGE_NAME,"direct");

        String queueName="code_queue";
        channel.queueDeclare(queueName,true,false,false,null);
        channel.queueBind(queueName,EXCHANGE_NAME,"my_routingKey");
    }


}
