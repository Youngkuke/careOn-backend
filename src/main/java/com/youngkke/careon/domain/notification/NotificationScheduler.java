package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.policy.CbInstitutionReader;
import com.youngkke.careon.domain.policy.CbInstitutionReader.CbInstitution;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final CbInstitutionReader cbInstitutionReader;

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
        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAll();
        Map<String, CbInstitution> cbByServId = loadCbInstitutions(savedPolicies);
        int created = 0;

        for (SavedPolicy savedPolicy : savedPolicies) {
            Carer carer = savedPolicy.getCarer();
            if (!carer.isNotificationEnabled()) {
                continue;
            }

            if (savedPolicy.isCbInstitution()) {
                created += createForCbInstitution(savedPolicy, cbByServId.get(savedPolicy.getServId()), today);
                continue;
            }

            Policy policy = savedPolicy.getPolicy();
            String policyName = policy.getPolicyName();
            created += createIfNeeded(savedPolicy, deadlineNotificationType(toDate(policy.getApplicationDeadline()), today), policyName);
            created += createIfNeeded(savedPolicy, resultNotificationType(toDate(policy.getResultDate()), today), policyName);
        }

        log.info("알림 자동 생성 배치 완료. {}건 생성.", created);
    }

    /**
     * cb(복지로) 제도의 마감일·발표일 알림.
     * 두 날짜 모두 AI 서버가 cb 테이블에 채우고, 우리는 해당 컬럼명이 설정돼 있을 때만 읽는다.
     * 설정이 없거나 값이 비어 있으면 알릴 시점을 계산할 수 없어 그 종류만 조용히 건너뛴다.
     */
    private int createForCbInstitution(SavedPolicy savedPolicy, CbInstitution institution, LocalDate today) {
        if (institution == null) {
            return 0;
        }
        String name = institution.name();
        return createIfNeeded(savedPolicy, deadlineNotificationType(institution.deadline(), today), name)
                + createIfNeeded(savedPolicy, resultNotificationType(institution.resultDate(), today), name);
    }

    private Map<String, CbInstitution> loadCbInstitutions(List<SavedPolicy> savedPolicies) {
        List<String> servIds = savedPolicies.stream()
                .map(SavedPolicy::getServId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return servIds.isEmpty() ? Map.of() : cbInstitutionReader.findAllByServIds(servIds);
    }

    private NotificationType deadlineNotificationType(LocalDate deadline, LocalDate today) {
        if (deadline == null) {
            return null;
        }
        long daysUntil = ChronoUnit.DAYS.between(today, deadline);
        return switch ((int) daysUntil) {
            case 7 -> NotificationType.DEADLINE_D7;
            case 3 -> NotificationType.DEADLINE_D3;
            case 1 -> NotificationType.DEADLINE_D1;
            default -> null;
        };
    }

    private LocalDate toDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private NotificationType resultNotificationType(LocalDate resultDate, LocalDate today) {
        if (resultDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(today, resultDate) == 0 ? NotificationType.RESULT_DDAY : null;
    }

    private int createIfNeeded(SavedPolicy savedPolicy, NotificationType type, String policyName) {
        if (type == null) {
            return 0;
        }
        if (notificationRepository.existsBySavedPolicyAndNotificationType(savedPolicy, type)) {
            // 알림은 이미 있으므로 새로 만들지 않는다. 시연 모드일 때만 푸시를 한 번 더 보낸다.
            if (repeatPush) {
                sendPush(savedPolicy, type, policyName);
            }
            return 0;
        }
        notificationRepository.save(Notification.builder()
                .savedPolicy(savedPolicy)
                .notificationType(type)
                .sentAt(LocalDateTime.now(KST))
                .read(false)
                .build());
        sendPush(savedPolicy, type, policyName);
        return 1;
    }

    /**
     * 앱 내 알림을 새로 만든 그 순간에만 푸시도 함께 보낸다.
     * 위의 (저장 제도, 알림 종류) 중복 체크를 그대로 타므로, 배치가 여러 번 돌아도 같은 알림은 1회만 나간다.
     */
    private void sendPush(SavedPolicy savedPolicy, NotificationType type, String policyName) {
        // 찜과 마찬가지로 제도를 가리키는 키가 두 갈래다. 앱이 어느 쪽인지 바로 알 수 있도록
        // 해당하는 키만 싣는다. (기존 제도면 policy_id, cb 제도면 serv_id)
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "POLICY_DEADLINE");
        data.put("notification_type", type.name());
        data.put("saved_policy_id", savedPolicy.getSavedPolicyId());
        if (savedPolicy.isCbInstitution()) {
            data.put("serv_id", savedPolicy.getServId());
        } else {
            data.put("policy_id", savedPolicy.getPolicy().getPolicyId());
        }
        data.put("url", "/notifications");

        pushSender.sendAfterCommit(
                savedPolicy.getCarer(),
                PushMessage.normal(type.pushTitle(), type.pushBody(policyName), data));
    }
}
