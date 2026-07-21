package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.notification.dto.NotificationResponse;
import com.youngkke.careon.domain.notification.dto.ReadAllResponse;
import com.youngkke.careon.domain.notification.dto.UnreadCountResponse;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CarerRepository carerRepository;

    /**
     * 알림 목록 조회 (최신순).
     * 이 API는 읽음 처리를 하지 않는다. 사용자가 알림 화면을 확인한 뒤 앱이 read-all을 별도로 호출한다.
     */
    public List<NotificationResponse> getList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now(DateTimes.KST);

        return notificationRepository.findAllWithPolicyByCarer(carer).stream()
                .map(notification -> toResponse(notification, now))
                .toList();
    }

    /** 미읽음 알림 개수 조회. 종 아이콘 뱃지용. */
    public UnreadCountResponse getUnreadCount(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return new UnreadCountResponse(notificationRepository.countUnreadByCarer(carer));
    }

    /** 모든 미읽음 알림을 읽음 처리한다. */
    @Transactional
    public ReadAllResponse readAll(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        List<Notification> unread = notificationRepository.findAllUnreadByCarer(carer);
        unread.forEach(Notification::markAsRead);

        return new ReadAllResponse(unread.size(), "모든 알림을 읽음 처리했습니다.");
    }

    private NotificationResponse toResponse(Notification notification, LocalDateTime now) {
        Policy policy = notification.getSavedPolicy().getPolicy();
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getSavedPolicy().getSavedPolicyId(),
                policy.getPolicyId(),
                policy.getPolicyName(),
                notification.getNotificationType().name(),
                DateTimes.toIsoString(notification.getSentAt()),
                notification.isRead(),
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

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
