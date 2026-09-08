package com.cjlogistics.mini.notification;

/**
 * 알림 종류. 요청 / 승낙 / 상태 변경을 구분한다.
 */
public enum NotificationType {
    /** 화주가 기사에게 배차를 제안(요청)함 -> 기사 수신 */
    DISPATCH_REQUESTED,
    /** 기사가 배차를 수락함 -> 화주 수신 */
    DISPATCH_ACCEPTED,
    /** 기사가 배차를 거절함 -> 화주 수신 */
    DISPATCH_REJECTED,
    /** 운송 상태가 변경됨 -> 화주 수신 */
    STATUS_CHANGED
}
