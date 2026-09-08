package com.cjlogistics.mini.notification;

/**
 * 알림 수신자 유형. 화주/기사 모두 알림을 받을 수 있도록 구분한다.
 * 값은 인증 토큰의 role(SHIPPER/DRIVER)과 동일하게 맞춘다.
 */
public enum RecipientType {
    SHIPPER,
    DRIVER;

    /** 인증 토큰의 role 문자열("SHIPPER"/"DRIVER")을 수신자 유형으로 변환한다. */
    public static RecipientType fromRole(String role) {
        return RecipientType.valueOf(role);
    }
}
