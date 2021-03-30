package pers.clare.core.message;

public interface GenericMessageService<T> {

    public T send(T body);

    public GenericReceiveListener<T> addListener(GenericReceiveListener<T> listener);
}
