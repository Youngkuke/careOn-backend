package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 보호자에게 푸시를 보낸다. 어떤 문구를 보낼지는 호출하는 도메인이 정하고, 여기선 "누구에게 언제 보내느냐"만 책임진다. */
@Component
@RequiredArgsConstructor
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);

    private final PushTokenRepository pushTokenRepository;
    private final ExpoPushClient expoPushClient;

    /**
     * 일반 알림(정책 마감 등). 보호자가 마이페이지에서 알림을 꺼두면 보내지 않는다.
     * 앱이 알림을 끌 때 토큰 삭제 API를 부르긴 하지만 그 요청이 실패했거나 다른 기기 토큰이 남아 있을 수 있어서,
     * 토큰 존재 여부만 믿지 않고 발송 직전에 설정을 한 번 더 확인한다.
     */
    public void sendAfterCommit(Carer carer, PushMessage message) {
        if (!carer.isNotificationEnabled()) {
            return;
        }
        sendToAllDevicesAfterCommit(carer, message);
    }

    /**
     * 긴급 알림(SOS·안심 구역 이탈). 사람 안전이 걸린 알림이라 notification_enabled와 무관하게 보낸다.
     * 대신 앱 마이페이지에서 "긴급 알림은 항상 발송됩니다"라고 안내하기로 했다. (설정을 몰래 무시하는 게 아니라 범위를 명시)
     */
    public void sendUrgentAfterCommit(Carer carer, PushMessage message) {
        sendToAllDevicesAfterCommit(carer, message);
    }

    /**
     * 보호자가 등록한 모든 기기로 푸시를 보낸다. 단, 트랜잭션이 열려 있으면 커밋된 뒤에 보낸다.
     * 롤백된 SOS로 푸시가 나가는 걸 막고, 외부 HTTP 호출이 끝날 때까지 DB 커넥션을 붙잡지 않기 위해서다.
     * 푸시 실패가 원래 작업(SOS 생성 등)을 되돌리면 안 되므로 예외는 여기서 삼키고 로그만 남긴다.
     */
    private void sendToAllDevicesAfterCommit(Carer carer, PushMessage message) {
        // 커밋 이후엔 영속성 컨텍스트가 닫혀 지연 로딩이 안 되므로, 토큰은 지금 미리 읽어둔다.
        List<String> tokens = pushTokenRepository.findAllByCarer(carer).stream()
                .map(PushToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(tokens, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(tokens, message);
            }
        });
    }

    private void send(List<String> tokens, PushMessage message) {
        try {
            List<String> expiredTokens = expoPushClient.send(tokens, message);
            if (!expiredTokens.isEmpty()) {
                pushTokenRepository.deleteAllByTokenIn(expiredTokens);
                log.info("만료된 푸시 토큰 {}건을 정리했습니다.", expiredTokens.size());
            }
        } catch (Exception e) {
            log.warn("푸시 발송에 실패했습니다. 대상 {}건.", tokens.size(), e);
        }
    }
}
