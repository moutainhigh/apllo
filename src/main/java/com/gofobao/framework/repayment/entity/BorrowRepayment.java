package com.gofobao.framework.repayment.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Zeke on 2017/5/26.
 */
@Entity(name = "BorrowRepayment")
@Data
@Table(name = "gfb_borrow_repayment")
@DynamicInsert
@DynamicUpdate
public class BorrowRepayment {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
    @Basic
    @Column(name = "borrow_id")
    private Long borrowId;
    @Basic
    @Column(name = "status")
    private Integer status;
    @Basic
    @Column(name = "`order`")
    private Integer order;
    @Basic
    @Column(name = "repay_money")
    private Long repayMoney;
    @Basic
    @Column(name = "principal")
    private Long principal;
    @Basic
    @Column(name = "interest")
    private Long interest;
    @Basic
    @Column(name = "repay_at")
    private Date repayAt;
    @Basic
    @Column(name = "repay_at_yes")
    private Date repayAtYes;
    @Basic
    @Column(name = "advance_at_yes")
    private Date advanceAtYes;
    @Basic
    @Column(name = "repay_money_yes")
    private Long repayMoneyYes;
    @Basic
    @Column(name = "advance_money_yes")
    private Long advanceMoneyYes;
    @Basic
    @Column(name = "late_days")
    private Integer lateDays;
    @Basic
    @Column(name = "late_interest")
    private Long lateInterest;
    @Basic
    @Column(name = "created_at")
    private Date createdAt;
    @Basic
    @Column(name = "updated_at")
    private Date updatedAt;
    @Basic
    @Column(name = "user_id")
    private Long userId;
    @Basic
    @Column(name = "iparam1")
    private Integer iparam1;
    @Basic
    @Column(name = "iparam2")
    private Integer iparam2;
    @Basic
    @Column(name = "iparam3")
    private Integer iparam3;
    @Basic
    @Column(name = "vparam1")
    private String vparam1;
    @Basic
    @Column(name = "vparam2")
    private String vparam2;
    @Basic
    @Column(name = "vparam3")
    private String vparam3;
    @Basic
    @Column(name = "t_user_id")
    private Long tUserId;
    private Boolean isAdvance;
    /**
     *     放款即信通信状态 0.未处理 1.处理中 2.处理失败 3.处理成功
     */
    private Integer repayStatus;
    private Date repayTriggerAt;
}
