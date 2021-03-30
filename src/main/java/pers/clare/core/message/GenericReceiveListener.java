package pers.clare.core.message;

@FunctionalInterface
public interface GenericReceiveListener<T> {
    void accept(MessageData message, T data);
}
