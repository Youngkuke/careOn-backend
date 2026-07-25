package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafeZoneRepository extends JpaRepository<SafeZone, Integer> {

    Optional<SafeZone> findByCared(Cared cared);
}
