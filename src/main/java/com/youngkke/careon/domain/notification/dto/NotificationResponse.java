package com.youngkke.careon.domain.notification.dto;

import java.time.OffsetDateTime;

/** 알림 목록 조회 응답 항목. */
public record NotificationResponse(
        Integer notificationId,
        Integer policyId,
        String policyName,
        String notificationType,
        OffsetDateTime sentAt,
        String relativeTime) {}
