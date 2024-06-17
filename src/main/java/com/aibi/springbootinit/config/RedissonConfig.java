package com.aibi.springbootinit.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")   //指定读取yml中的哪一个配置,然后spring会自动投射在下边
@Data
public class RedissonConfig {

    private String host;

    private String port="6379";

    @Bean
    public RedissonClient redissonClient() throws IOException {
        // 1. 设置配置
        Config config = new Config();
        String redisAddr =String.format("redis://%s:%s",host,port);
        config.useSingleServer().setAddress(redisAddr).setPassword("fujia").setDatabase(9).setPingConnectionInterval(1000);

        //2.创建实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
