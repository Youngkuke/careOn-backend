package com.youngkke.careon.domain.timeline;

import com.youngkke.careon.domain.carer.Cared;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 타임라인에 사건을 남긴다.
 *
 * <p>사건이 일어난 도메인 트랜잭션 안에서 그대로 호출한다. 원래 작업이 롤백되면 타임라인 기록도 함께 사라져야
 * 하기 때문이다. (푸시는 커밋 뒤에 보내는 것과 반대로, 기록은 같은 트랜잭션에 묶는다.)
 */
@Component
@RequiredArgsConstructor
public class CareEventRecorder {

    private final CareEventRepository careEventRepository;

    public void record(Cared cared, CareEventType type, LocalDateTime occurredAt, Integer refId) {
        careEventRepository.save(CareEvent.builder()
                .cared(cared)
                .type(type)
                .occurredAt(occurredAt)
                .refId(refId)
                .summary(type.getSummary())
                .build());
    }

    /** 대응하는 원본 이벤트 행이 없는 사건(워치 연결·해제, 위치 공유 시작·종료)용. */
    public void record(Cared cared, CareEventType type, LocalDateTime occurredAt) {
        record(cared, type, occurredAt, null);
    }
}
