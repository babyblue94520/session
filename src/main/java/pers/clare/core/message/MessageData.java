package pers.clare.core.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageData<T> {
    private Long time;
    private String origin;
    private T data;
}
