package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.policy.SavedPolicyRepository;
import com.youngkke.careon.domain.carer.Carer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장한 제도들의 마감일/발표일이 다가오면 알림을 자동 생성하는 배치.
 * 명세서엔 없는, 실제 알림 기능이 동작하려면 필요해서 추가한 부분.
 * 매일 오전 9시(KST)에 한 번 돌면서 D-7/D-3/D-1(마감일), D-Day(발표일) 조건에 맞는 저장 제도에
 * 아직 같은 타입 알림이 없으면 새로 만든다. notificationEnabled를 꺼둔 유저는 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SavedPolicyRepository savedPolicyRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateNotifications() {
        LocalDate today = LocalDate.now(KST);
        int created = 0;

        for (SavedPolicy savedPolicy : savedPolicyRepository.findAll()) {
            Carer carer = savedPolicy.getCarer();
            if (!carer.isNotificationEnabled()) {
                continue;
            }

            Policy policy = savedPolicy.getPolicy();
            created += createIfNeeded(savedPolicy, deadlineNotificationType(policy, today));
            created += createIfNeeded(savedPolicy, resultNotificationType(policy, today));
        }

        log.info("알림 자동 생성 배치 완료. {}건 생성.", created);
    }

    private NotificationType deadlineNotificationType(Policy policy, LocalDate today) {
        if (policy.getApplicationDeadline() == null) {
            return null;
        }
        long daysUntil = ChronoUnit.DAYS.between(today, policy.getApplicationDeadline().toLocalDate());
        return switch ((int) daysUntil) {
            case 7 -> NotificationType.DEADLINE_D7;
            case 3 -> NotificationType.DEADLINE_D3;
            case 1 -> NotificationType.DEADLINE_D1;
            default -> null;
        };
    }

    private NotificationType resultNotificationType(Policy policy, LocalDate today) {
        if (policy.getResultDate() == null) {
            return null;
        }
        long daysUntil = ChronoUnit.DAYS.between(today, policy.getResultDate().toLocalDate());
        return daysUntil == 0 ? NotificationType.RESULT_DDAY : null;
    }

    private int createIfNeeded(SavedPolicy savedPolicy, NotificationType type) {
        if (type == null) {
            return 0;
        }
        if (notificationRepository.existsBySavedPolicyAndNotificationType(savedPolicy, type)) {
            return 0;
        }
        notificationRepository.save(Notification.builder()
                .savedPolicy(savedPolicy)
                .notificationType(type)
                .sentAt(LocalDateTime.now(KST))
                .read(false)
                .build());
        return 1;
    }
}
