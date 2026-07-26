package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.carer.Carer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PushTokenRepository extends JpaRepository<PushToken, Integer> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByCarer(Carer carer);

    @Transactional
    void deleteByCarerAndToken(Carer carer, String token);

    /** 회원 탈퇴 시 FK 제약 위반을 막기 위해 함께 정리한다. */
    @Transactional
    void deleteAllByCarer(Carer carer);

    /**
     * Expo가 "더 이상 유효하지 않은 토큰"이라고 답한 것들을 정리한다.
     * 커밋 직후(afterCommit) 호출되는데, 이때는 이미 커밋이 끝난 트랜잭션이 아직 스레드에 매달려 있다.
     * 기본 propagation이면 그 트랜잭션에 합류해버려 삭제가 반영되지 않으므로 새 트랜잭션을 연다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void deleteAllByTokenIn(Collection<String> tokens);
}
