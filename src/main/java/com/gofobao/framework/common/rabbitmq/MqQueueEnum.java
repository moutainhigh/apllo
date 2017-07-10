package com.gofobao.framework.common.rabbitmq;

/**
 * Created by Max on 17/5/26.
 */
public enum MqQueueEnum {
    RABBITMQ_SMS(MqQueueEnumContants.RABBITMQ_SMS),
    RABBITMQ_EMAIL(MqQueueEnumContants.RABBITMQ_EMAIL),
    RABBITMQ_AUTO_TENDER(MqQueueEnumContants.RABBITMQ_AUTO_TENDER),
    RABBITMQ_NOTICE(MqQueueEnumContants.RABBITMQ_NOTICE) ,
    RABBITMQ_BORROW(MqQueueEnumContants.RABBITMQ_BORROW) ,
    RABBITMQ_USER_ACTIVE(MqQueueEnumContants.RABBITMQ_USER_ACTIVE) ,
    RABBITMQ_ACTIVITY(MqQueueEnumContants.RABBITMQ_ACTIVITY) ,
    RABBITMQ_RED_PACKAGE(MqQueueEnumContants.RABBITMQ_RED_PACKAGE)  ,
    RABBITMQ_CREDIT(MqQueueEnumContants.RABBITMQ_CREDIT)  ;
    private String value ;

    MqQueueEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}


