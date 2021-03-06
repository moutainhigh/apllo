package com.gofobao.framework.lend.vo.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by admin on 2017/6/12.
 */
@Data
public class LendInfo {

    @ApiModelProperty("出借人")
    private String userName;

    @ApiModelProperty("剩余金额")
    private String surplusMoney;

    @ApiModelProperty("剩余金额隐藏")
    private Integer surplusMoneyHide;

    @ApiModelProperty("起借金额")
    private String startMoney;

    @ApiModelProperty("起借金额隐藏")
    private Integer startMoneyHide;

    @ApiModelProperty("年华利率")
    private String apr;

    @ApiModelProperty("期限")
    private String  timeLimit;

    @ApiModelProperty("还款时间")
    private String collectionAt;

    @ApiModelProperty("还款时间")
    private String  repayAtYes;

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("信用额度")
    private String equityLimit;

    @ApiModelProperty("信用额度隐藏")
    private Long equityLimitHide;

    @ApiModelProperty("状态（0、可借；1、结束）")
    private Integer status;






}
