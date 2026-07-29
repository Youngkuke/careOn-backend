package com.youngkke.careon.domain.notification.dto;

/**
 * 알림 목록 조회(앱) 응답 항목.
 *
 * <p>제도가 두 갈래라 기존 제도는 policyId가, cb 제도는 servId가 채워진다. 둘 중 하나는 항상 null이다.
 */
public record NotificationResponse(
        Integer notificationId,
        Integer savedPolicyId,
        Integer policyId,
        String servId,
        String policyName,
        String notificationType,
        String sentAt,
        boolean isRead,
        String relativeTime) {}
