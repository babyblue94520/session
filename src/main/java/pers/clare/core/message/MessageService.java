package pers.clare.core.message;

import java.util.function.Consumer;

public interface MessageService {

    void send(String topic, String body);

    void listener(String topic, Consumer<String> listener);

    void onConnected(Runnable runnable);
}
