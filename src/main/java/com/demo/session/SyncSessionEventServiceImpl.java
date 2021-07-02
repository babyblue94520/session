package com.demo.session;

import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;
import pers.clare.session.AbstractSyncSessionEventService;
import com.demo.redis.listener.MyRedisMessageListenerContainer;

import java.util.function.Consumer;

@Service
public class SyncSessionEventServiceImpl extends AbstractSyncSessionEventService implements InitializingBean {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MyRedisMessageListenerContainer listenerContainer;

    @Autowired
    private DefaultClientResources defaultClientResources;

    @Override
    public void afterPropertiesSet() throws Exception {
        defaultClientResources.eventBus().get().subscribe((event) -> {
            if (event instanceof ConnectedEvent) {
                publishConnectedEvent();
            }
        });
    }

    @Override
    public String send(String topic, String body) {
        stringRedisTemplate.convertAndSend(topic, body);
        return topic;
    }

    @Override
    public Consumer<String> addListener(String topic, Consumer<String> listener) {
        listenerContainer.addMessageListener((message, pattern) -> {
            listener.accept(new String(message.getBody()));
        }, new PatternTopic(topic));
        return listener;
    }
}
