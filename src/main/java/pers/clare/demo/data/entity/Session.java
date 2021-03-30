package pers.clare.demo.data.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Setter
@Getter
@Entity
public class Session implements Serializable {
    @Id
    @Column(columnDefinition = "varchar(32)")
    private String id;

    @Column(columnDefinition = "bigint default '0'")
    private long createTime;

    @Column(columnDefinition = "bigint default '0'")
    private long maxInactiveInterval;

    @Column(columnDefinition = "bigint default '0'")
    private long lastAccessTime;

    @Column(columnDefinition = "varchar(30) default ''")
    private String username;

    @Column(columnDefinition = "text default '{}'")
    private String attributes;

}
