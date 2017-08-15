package com.gofobao.framework.common.rabbitmq;

/**
 * Created by Max on 17/5/26.
 */
public enum MqTagEnum {
    SMS_WITHDRAW_CASH("SMS_WITHDRAW_CASH"),
    SMS_RESET_PASSWORD("SMS_RESET_PASSWORD"),
    SMS_SWICTH_PHONE("SMS_SWICTH_PHONE"),
    SMS_BUNDLE("SMS_BUNDLE"),
    SMS_MODIFY_BANK("SMS_MODIFY_BANK"),
    SMS_REST_PAY_PASSWORD("SMS_REST_PAY_PASSWORD"),

    SMS_DEFAULT("SMS_DEFAULT"),
    SMS_EMAIL_BIND("SMS_EMAIL_BIND"),
    SEND_BORROW_PROTOCOL_EMAIL("SEND_BORROW_PROTOCOL_EMAIL"),
    SMS_RESET_PAY_PASSWORD("SMS_RESET_PAY_PASSWORD"),
    SMS_BORROW_SUCCESS("SMS_BORROW_SUCCESS"),
    SMS_RECEIVED_REPAY("SMS_RECEIVED_REPAY"),
    SMS_BORROW_REPAYMENT_PUSH("SMS_BORROW_REPAYMENT_PUSH"),
    SMS_REGISTER("SMS_REGISTER"),
    SMS_WINDMILL_USER_REGISTER("SMS_WINDMILL_USER_REGISTER"), //风车理财用户注册
    USER_ACTIVE_REGISTER("SMS_REGISTER"),  // 用户注册
    FIRST_VERIFY("FIRST_VERIFY"), //初审
    AGAIN_VERIFY("AGAIN_VERIFY"), //复审
    AUTO_TENDER("AUTO_TENDER"), //自动投标
    AGAIN_VERIFY_TRANSFER("AGAIN_VERIFY_TRANSFER"),//债权转让复审
    AUTO_TRANSFER("AUTO_TRANSFER"), //自动债权转让
    NOTICE_PUBLISH("NOTICE_PUBLISH"), // 站内信通知
    NOTICE_PUSH("NOTICE_PUSH"), // 资金变动
    RECHARGE("RECHARGE"), // 充值
    LOGIN("LOGIN"), // 登录
    GIVE_COUPON("GIVE_COUPON"), // 赠送流量券
    MARKETING_OPEN_ACCOUNT("MARKETING_OPEN_ACCOUNT") , // 开户
    MARKETING_TENDER("MARKETING_TENDER"), // 投标
    END_CREDIT_BY_NOT_TRANSFER("END_CREDIT_BY_NOT_TRANSFER"),//结束债权非转让
    END_CREDIT_BY_TRANSFER("END_CREDIT_BY_TRANSFER"),//结束债权
    END_CREDIT_ALL("END_CREDIT_ALL"),//结束债权
    BATCH_DEAL("batchDeal"),//批次处理
    REPAY_ALL("repayAll"),//提前结清
    REPAY_ADVANCE("repayAdvance"),//提前结清
    ADVANCE("advance"),//名义借款人垫付
    REPAY("repay");//立即还款


    private String value;

    private MqTagEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

