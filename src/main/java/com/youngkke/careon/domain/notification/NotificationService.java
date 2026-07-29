package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.notification.dto.NotificationResponse;
import com.youngkke.careon.domain.notification.dto.ReadAllResponse;
import com.youngkke.careon.domain.notification.dto.UnreadCountResponse;
import com.youngkke.careon.domain.policy.CbInstitutionReader;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CarerRepository carerRepository;
    private final CbInstitutionReader cbInstitutionReader;

    /**
     * 알림 목록 조회 (최신순).
     * 이 API는 읽음 처리를 하지 않는다. 사용자가 알림 화면을 확인한 뒤 앱이 read-all을 별도로 호출한다.
     */
    public List<NotificationResponse> getList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now(DateTimes.KST);

        List<Notification> notifications = notificationRepository.findAllWithPolicyByCarer(carer);
        Map<String, CbInstitutionReader.CbInstitution> cbInstitutions =
                cbInstitutionReader.findAllByServIds(notifications.stream()
                        .map(notification -> notification.getSavedPolicy().getServId())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList());

        return notifications.stream()
                .map(notification -> toResponse(notification, cbInstitutions, now))
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

    /**
     * cb 제도의 알림은 policy가 없어 제도명을 cb에서 읽는다.
     * 원본 행을 못 찾더라도 알림 자체는 목록에 남긴다. 사용자가 이미 받은 알림이 화면에서 사라지면
     * 뱃지 숫자와 목록이 어긋나 보이기 때문이다.
     */
    private NotificationResponse toResponse(
            Notification notification,
            Map<String, CbInstitutionReader.CbInstitution> cbInstitutions,
            LocalDateTime now) {
        SavedPolicy savedPolicy = notification.getSavedPolicy();
        Integer policyId = null;
        String policyName = null;

        if (savedPolicy.isCbInstitution()) {
            CbInstitutionReader.CbInstitution institution = cbInstitutions.get(savedPolicy.getServId());
            policyName = institution == null ? null : institution.name();
        } else {
            Policy policy = savedPolicy.getPolicy();
            policyId = policy.getPolicyId();
            policyName = policy.getPolicyName();
        }

        return new NotificationResponse(
                notification.getNotificationId(),
                savedPolicy.getSavedPolicyId(),
                policyId,
                savedPolicy.getServId(),
                policyName,
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
