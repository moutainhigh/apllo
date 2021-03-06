package com.gofobao.framework.financial.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "gfb_aleve")
@Entity
@DynamicUpdate
@DynamicInsert
@Data
public class Aleve {
    @Id
    @GeneratedValue
    private Long id;
    private String bank;
    private String cardnbr;
    private String amount;
    private String curNum;
    private String crflag;
    private String valdate;
    private String inpdate;
    private String reldate;
    private String inptime;
    private String tranno;
    private String oriTranno;
    private String transtype;
    private String desline;
    private String currBal;
    private String forcardnbr;
    private String revind;
    private String resv;
    private String queryDate ;
    private Date createAt;
}
