package com.gofobao.framework.message.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by Max on 17/5/17.
 */
@Entity
@DynamicInsert
@Table(name = "gfb_sms_notice_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsNoticeSettingsEntity implements Serializable{
    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private int userId ;
    @Basic
    @Column(name = "received_repay")
    private byte receivedRepay;
    @Basic
    @Column(name = "borrow_success")
    private byte borrowSuccess;
}
