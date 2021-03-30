package pers.clare.core.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
public class RequestSession {
    @JsonIgnore
    String id;
    @JsonIgnore
    long createTime;
    @JsonIgnore
    long maxInactiveInterval;
    @Setter(AccessLevel.PACKAGE)
    @JsonIgnore
    long lastAccessTime;

    @JsonIgnore
    protected String username;

    String userAgent;

    String ip;

    @JsonIgnore
    volatile boolean valid = true;

    @JsonIgnore
    long lastUpdateAccessTime;

    @JsonIgnore
    volatile int refresh = 0;

    @JsonIgnore
    volatile long lastCountRefresh = 0;

    public void setUsername(String username) {
        this.username = username;
        this.save();
    }

    public void invalidate() {
        RequestCacheHolder.get().invalidate();
    }

    protected void save() {
        RequestCacheHolder.get().save();
    }
}
