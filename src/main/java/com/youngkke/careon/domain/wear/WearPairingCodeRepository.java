package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WearPairingCodeRepository extends JpaRepository<WearPairingCode, Integer> {

    Optional<WearPairingCode> findFirstByCodeHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String codeHash, LocalDateTime now);

    boolean existsByCodeHashAndUsedAtIsNullAndExpiresAtAfter(String codeHash, LocalDateTime now);

    /**
     * 회원 탈퇴 정리용. 페어링을 시도할 때마다 코드가 새로 쌓이므로 한 번에 지운다.
     * created_by_carer_id도 carers를 참조하지만, 보호자는 자기 피보호자만 페어링하므로 피보호자 기준으로 다 걸린다.
     */
    @Modifying
    @Query("delete from WearPairingCode p where p.cared in :caredList")
    void deleteAllByCaredIn(@Param("caredList") List<Cared> caredList);
}
