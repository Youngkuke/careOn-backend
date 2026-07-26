package com.youngkke.careon.domain.wear.dto;

/**
 * 기기 상태 보고 결과.
 *
 * @param lowBatteryNotified 이번 보고로 보호자에게 배터리 부족 알림이 나갔는지. 워치가 같은 내용을 자체적으로
 *     또 띄우지 않도록 알려준다.
 */
public record WearDeviceStatusReportResponse(Integer batteryPercent, boolean lowBatteryNotified) {}
