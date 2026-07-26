package com.youngkke.careon.domain.wear.dto;

/**
 * 실시간 위치 공유 시작/중지 결과.
 *
 * @param expiresAt 자동 종료 시각. 껐을 때는 null.
 * @param updatedAt 이미 배포된 앱이 읽고 있어서 남겨둔 필드. 앱이 expiresAt으로 옮겨간 뒤 제거한다.
 */
public record LiveLocationTrackingResponse(
        boolean enabled, String expiresAt, int intervalSeconds, String updatedAt) {}
