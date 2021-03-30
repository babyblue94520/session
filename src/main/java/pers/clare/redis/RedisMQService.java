package pers.clare.redis;

import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.stereotype.Service;
import pers.clare.core.message.AbstractMessageService;
import pers.clare.redis.listener.MyRedisMessageListenerContainer;

import java.util.function.Consumer;

@Log4j2
@Service(RedisMQService.BEAN_NAME)
public class RedisMQService extends AbstractMessageService implements InitializingBean {
    public static final String BEAN_NAME = "redisMessageService";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MyRedisMessageListenerContainer myRedisMessageListenerContainer;

    @Autowired
    private DefaultClientResources clientResources;

    @Override
    public void afterPropertiesSet() throws Exception {
        clientResources.eventBus().get().subscribe(event -> {
            if (event instanceof ConnectedEvent) {
                connectedListener.forEach(Runnable::run);
            }
        });
    }

    @Override
    public void send(String topic, String body) {
        stringRedisTemplate.convertAndSend(topic, body);
    }

    @Override
    public void listener(String topic, Consumer<String> listener) {
        myRedisMessageListenerContainer.addMessageListener((message, pattern) -> {
            log.trace("pattern:{},message:{}", new String(pattern), message);
            listener.accept(new String(message.getBody()));
        }, new PatternTopic(topic));
    }

}
