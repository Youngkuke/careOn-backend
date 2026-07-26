package com.youngkke.careon.domain.caretask;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareTaskCompletionRepository extends JpaRepository<CareTaskCompletion, Integer> {

    Optional<CareTaskCompletion> findByCareTaskAndCompletedDate(CareTask careTask, LocalDate completedDate);

    /** 오늘 목록의 완료 여부를 한 번에 채우기 위한 조회. 항목마다 따로 묻지 않으려는 것이다. */
    List<CareTaskCompletion> findAllByCareTaskInAndCompletedDate(List<CareTask> careTasks, LocalDate completedDate);

    void deleteByCareTaskAndCompletedDate(CareTask careTask, LocalDate completedDate);
}
