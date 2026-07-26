package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.SafeZoneEventStatus;
import com.youngkke.careon.domain.wear.SafeZoneResponseType;

/**
 * 워치가 재시작된 뒤 이탈 화면을 복원하기 위한 응답.
 * 보호자 앱용(ActiveSafeZoneEventResponse)과 달리 caredId를 담지 않고, 대신 응답 마감 시각을 준다.
 *
 * @param responseDeadlineAt 이 시각까지 USER_OKAY/NEED_HELP를 고를 수 있다. detectedAt에 서버 상수를 더해 계산하며,
 *     따로 저장하지 않는다.
 */
public record WearActiveSafeZoneEventResponse(
        Integer eventId,
        Integer safeZoneId,
        SafeZoneEventStatus status,
        SafeZoneResponseType response,
        String detectedAt,
        String responseDeadlineAt,
        SafeZoneEventLocationResponse location) {}
