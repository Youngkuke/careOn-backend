package com.youngkke.careon.domain.carer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarerRepository extends JpaRepository<Carer, Integer> {

    Optional<Carer> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Carer> findByResetToken(String resetToken);
}
