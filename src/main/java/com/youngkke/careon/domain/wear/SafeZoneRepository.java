package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafeZoneRepository extends JpaRepository<SafeZone, Integer> {

    Optional<SafeZone> findByCared(Cared cared);

    /** 회원 탈퇴 정리용. 피보호자당 한 건이라 건별로 지워도 부담이 없다. */
    void deleteAllByCaredIn(List<Cared> caredList);
}
