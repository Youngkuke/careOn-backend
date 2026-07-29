package com.youngkke.careon.domain.caretask;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareTaskCompletionRepository extends JpaRepository<CareTaskCompletion, Integer> {

    Optional<CareTaskCompletion> findByCareTaskAndCompletedDate(CareTask careTask, LocalDate completedDate);

    /** 오늘 목록의 완료 여부를 한 번에 채우기 위한 조회. 항목마다 따로 묻지 않으려는 것이다. */
    List<CareTaskCompletion> findAllByCareTaskInAndCompletedDate(List<CareTask> careTasks, LocalDate completedDate);

    void deleteByCareTaskAndCompletedDate(CareTask careTask, LocalDate completedDate);

    /** 회원 탈퇴 정리용. 할 일 하나당 날짜별로 쌓이는 기록이라 한 번에 지운다. */
    @Modifying
    @Query("delete from CareTaskCompletion c where c.careTask in :careTasks")
    void deleteAllByCareTaskIn(@Param("careTasks") List<CareTask> careTasks);
}
