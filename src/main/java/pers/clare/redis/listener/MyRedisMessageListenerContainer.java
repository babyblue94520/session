package pers.clare.redis.listener;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

public class MyRedisMessageListenerContainer extends RedisMessageListenerContainer {

    /**
     * 服務啟動時，Redis 連線失敗就不會重新連線
     * 包裝成 RedisConnectionFailureException 讓重新連線正常
     *
     * @param ex
     */
    @Override
    protected void handleSubscriptionException(Throwable ex) {
        super.handleSubscriptionException(new RedisConnectionFailureException(ex.getMessage(), ex));
    }
}
