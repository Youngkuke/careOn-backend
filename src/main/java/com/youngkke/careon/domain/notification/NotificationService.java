package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.notification.dto.NotificationResponse;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    private final NotificationRepository notificationRepository;
    private final CarerRepository carerRepository;

    /** 알림 목록 조회 (최신순). 조회와 동시에 전부 읽음 처리한다 (별도 읽음 처리 API 없음). */
    @Transactional
    public List<NotificationResponse> getList(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        List<Notification> notifications = notificationRepository.findAllBySavedPolicy_CarerOrderBySentAtDesc(carer);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<NotificationResponse> responses =
                notifications.stream().map(n -> toResponse(n, now)).toList();

        notifications.forEach(Notification::markAsRead);

        return responses;
    }

    private NotificationResponse toResponse(Notification notification, LocalDateTime now) {
        Policy policy = notification.getSavedPolicy().getPolicy();
        return new NotificationResponse(
                notification.getNotificationId(),
                policy.getPolicyId(),
                policy.getPolicyName(),
                notification.getNotificationType().name(),
                notification.getSentAt().atOffset(KST),
                toRelativeTime(notification.getSentAt(), now));
    }

    private String toRelativeTime(LocalDateTime sentAt, LocalDateTime now) {
        Duration duration = Duration.between(sentAt, now);
        long seconds = Math.max(duration.getSeconds(), 0);

        if (seconds < 60) {
            return "방금 전";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        long days = hours / 24;
        if (days < 7) {
            return days + "일 전";
        }
        if (days < 30) {
            return (days / 7) + "주 전";
        }
        if (days < 365) {
            return (days / 30) + "개월 전";
        }
        return (days / 365) + "년 전";
    }

    private Carer getCarerOrThrow(Integer userId) {
        return carerRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
