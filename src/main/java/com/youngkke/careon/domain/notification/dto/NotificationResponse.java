package com.youngkke.careon.domain.notification.dto;

/** 알림 목록 조회(앱) 응답 항목. */
public record NotificationResponse(
        Integer notificationId,
        Integer savedPolicyId,
        Integer policyId,
        String policyName,
        String notificationType,
        String sentAt,
        boolean isRead,
        String relativeTime) {}
