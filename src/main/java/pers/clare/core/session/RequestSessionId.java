package pers.clare.core.session;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestSessionId {
    private String id;

    private String username;
}
