package com.cjlogistics.mini.dispatch.dto;

/**
 * 배차 생성 요청. driverId 를 지정하면 해당 기사로 배차하고,
 * null 이면 최적 후보를 자동 선택한다.
 */
public record DispatchCreateRequest(Long driverId) {
}
