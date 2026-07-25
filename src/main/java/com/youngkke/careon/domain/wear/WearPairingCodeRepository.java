package com.youngkke.careon.domain.wear;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearPairingCodeRepository extends JpaRepository<WearPairingCode, Integer> {

    Optional<WearPairingCode> findFirstByCodeHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String codeHash, LocalDateTime now);

    boolean existsByCodeHashAndUsedAtIsNullAndExpiresAtAfter(String codeHash, LocalDateTime now);
}
