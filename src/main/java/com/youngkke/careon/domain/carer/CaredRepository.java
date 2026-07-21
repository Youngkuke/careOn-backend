package com.youngkke.careon.domain.carer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaredRepository extends JpaRepository<Cared, Integer> {

    List<Cared> findAllByCarerOrderByCaredIdAsc(Carer carer);

    Optional<Cared> findByCaredIdAndCarer(Integer caredId, Carer carer);
}
