package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.policy.SavedPolicyRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.push.PushMessage;
import com.youngkke.careon.domain.push.PushSender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장한 제도들의 마감일/발표일이 다가오면 알림을 자동 생성하는 배치.
 * 명세서엔 없는, 실제 알림 기능이 동작하려면 필요해서 추가한 부분.
 * 매일 오전 9시(KST)에 한 번 돌면서 D-7/D-3/D-1(마감일), D-Day(발표일) 조건에 맞는 저장 제도에
 * 아직 같은 타입 알림이 없으면 새로 만들고, 같은 내용을 푸시로도 보낸다.
 * notificationEnabled를 꺼둔 유저는 앱 내 알림도 푸시도 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SavedPolicyRepository savedPolicyRepository;
    private final NotificationRepository notificationRepository;
    private final PushSender pushSender;

    /**
     * 이미 만들어둔 알림도 배치가 돌 때마다 푸시를 다시 보낼지 여부.
     * 기본값 false. 같은 마감 알림이 하루에 여러 번 울리면 사용자에겐 그냥 스팸이기 때문이다.
     * 시연처럼 "정해진 시각에 알림이 뜨는 장면"을 여러 번 보여줘야 할 때만 켠다.
     * 켜도 앱 내 알림 목록은 그대로 1건만 유지된다 (푸시만 다시 나간다).
     */
    @Value("${notification.repeat-push:false}")
    private boolean repeatPush;

    /**
     * 배치가 도는 시각. 기본은 매일 오전 9시(KST)로, 마감 알림을 받기 자연스러운 시간대라 정한 값이다.
     * 시연 등으로 시각을 옮겨야 하면 코드 대신 application.yaml의 notification.deadline-cron을 고친다.
     * (예: 오후 2·3·4시 = "0 0 14,15,16 * * *")
     */
    @Scheduled(cron = "${notification.deadline-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void generateNotifications() {
        LocalDate today = LocalDate.now(KST);
        int created = 0;

        for (SavedPolicy savedPolicy : savedPolicyRepository.findAll()) {
            Carer carer = savedPolicy.getCarer();
            if (!carer.isNotificationEnabled()) {
                continue;
            }
            // cb 제도는 마감일·발표일 데이터가 없어 알릴 시점 자체를 계산할 수 없다.
            // 건너뛰지 않으면 policy가 비어 있어 이 배치 전체가 죽는다.
            if (savedPolicy.isCbInstitution()) {
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
            // 알림은 이미 있으므로 새로 만들지 않는다. 시연 모드일 때만 푸시를 한 번 더 보낸다.
            if (repeatPush) {
                sendPush(savedPolicy, type);
            }
            return 0;
        }
        notificationRepository.save(Notification.builder()
                .savedPolicy(savedPolicy)
                .notificationType(type)
                .sentAt(LocalDateTime.now(KST))
                .read(false)
                .build());
        sendPush(savedPolicy, type);
        return 1;
    }

    /**
     * 앱 내 알림을 새로 만든 그 순간에만 푸시도 함께 보낸다.
     * 위의 (저장 제도, 알림 종류) 중복 체크를 그대로 타므로, 배치가 여러 번 돌아도 같은 알림은 1회만 나간다.
     */
    private void sendPush(SavedPolicy savedPolicy, NotificationType type) {
        Policy policy = savedPolicy.getPolicy();
        pushSender.sendAfterCommit(
                savedPolicy.getCarer(),
                PushMessage.normal(
                        type.pushTitle(),
                        type.pushBody(policy.getPolicyName()),
                        Map.of(
                                "type", "POLICY_DEADLINE",
                                "notification_type", type.name(),
                                "saved_policy_id", savedPolicy.getSavedPolicyId(),
                                "policy_id", policy.getPolicyId(),
                                "url", "/notifications")));
    }
}
