package com.youngkke.careon.domain.carer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarerIncomeSignalRepository extends JpaRepository<CarerIncomeSignal, Integer> {

    List<CarerIncomeSignal> findAllByCarerOrderBySignalIdAsc(Carer carer);

    Optional<CarerIncomeSignal> findBySignalIdAndCarer(Integer signalId, Carer carer);
}
