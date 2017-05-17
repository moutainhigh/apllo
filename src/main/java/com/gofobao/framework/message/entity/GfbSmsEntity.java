package com.gofobao.framework.message.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by Max on 17/5/17.
 */
@Entity
@Table(name = "gfb_sms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GfbSmsEntity {
    @Id
    @Column(name = "id")
    private int id;
    @Basic
    @Column(name = "username")
    private String username;
    @Basic
    @Column(name = "type")
    private String type;
    @Basic
    @Column(name = "phone")
    private String phone;
    @Basic
    @Column(name = "content")
    private String content;
    @Basic
    @Column(name = "ext")
    private String ext;
    @Basic
    @Column(name = "stime")
    private String stime;

    @Basic
    @Column(name = "rrid")
    private String rrid;

    @Basic
    @Column(name = "status")
    private byte status;

    @Basic
    @Column(name = "ip")
    private String ip;

    @Basic
    @Column(name = "created_at")
    private Timestamp createdAt;
}
