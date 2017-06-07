package com.gofobao.framework.api.helper;

/**
 * 即信交易类型
 * Created by Max on 17/5/19.
 */

public enum JixinTxCodeEnum {
    /**
     * 开户增强
     */
    OPEN_ACCOUNT_PLUS(
            "accountOpenPlus",
            "/escrow/p2p/online"),

    /**
     * 发送短信验证码
     */
    SMS_CODE_APPLY(
            "smsCodeApply",
            "/escrow/p2p/online"),


    BID_AUTO_APPLY(
            "bidAutoApply",
            "/escrow/p2p/online"
    ),

    /**
     * 初始化密码
     */
    PASSWORD_SET(
            "passwordSet",
            "/escrow/p2p/page/passwordset"),

    /**
     * 充值密码
     */
    PASSWORD_RESET(
            "passwordReset",
            "/escrow/p2p/page/mobile"),

    /**
     * 红包发放
     */
    SEND_RED_PACKET(
            "voucherPay",
            "/escrow/p2p/online"),

    /**
     * 标的登记
     */
    DEBT_REGISTER(
            "debtRegister",
            "/escrow/p2p/online"),


    /**
     * 自动投标
     */
    AUTO_BID_AUTH_PLUS(
            "autoBidAuthPlus",
            "/escrow/p2p/page/mobile/plus"),
    /**
     * 债权转让
     */
    AUTO_CREDIT_INVEST_AUTH_PLUS(
            "autoCreditInvestAuthPlus",
            "/escrow/p2p/page/mobile/plus"),

    /**
     * 借款人标的撤销
     */
    DEBT_REGISTER_CANCEL(
            "debtRegisterCancel",
            "/escrow/p2p/online"),
    /**
     * 借款人标的信息查询
     */
    DEBT_DETAILS_QUERY(
            "debtDetailsQuery",
            "/escrow/p2p/online");

    private String value;
    private String url;

    private JixinTxCodeEnum(String value, String url) {
        this.value = value;
        this.url = url;
    }

    public String getValue() {
        return value;
    }

    public String getUrl() {
        return url;
    }
}
