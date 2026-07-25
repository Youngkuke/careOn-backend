package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearDeviceRepository extends JpaRepository<WearDevice, Integer> {

    Optional<WearDevice> findByCared(Cared cared);
}
