package com.demo.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.demo.redis.listener.MyRedisMessageListenerContainer;


@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(connectionFactory);
        return stringRedisTemplate;
    }

    @Bean
    public MyRedisMessageListenerContainer container(
    		RedisConnectionFactory connectionFactory
    ) {
        MyRedisMessageListenerContainer container = new MyRedisMessageListenerContainer();
        container.setRecoveryInterval(5000L);
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
