package com.youngkke.careon.domain.wear.dto;

/**
 * 워치 연결 상태.
 *
 * @param connectedAt pairedAt과 같은 값이다. 이미 배포된 앱이 이 필드를 읽고 있어서 남겨두고,
 *     프론트가 요청한 이름인 pairedAt을 함께 내보낸다. 앱이 pairedAt으로 옮겨간 뒤 제거한다.
 */
public record WearDeviceStatusResponse(
        Integer wearDeviceId,
        boolean connected,
        String deviceName,
        String pairedAt,
        String connectedAt,
        String lastSeenAt) {}
