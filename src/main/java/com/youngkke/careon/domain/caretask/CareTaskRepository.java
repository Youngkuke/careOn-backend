package com.youngkke.careon.domain.caretask;

import com.youngkke.careon.domain.carer.Cared;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareTaskRepository extends JpaRepository<CareTask, Integer> {

    /** 보호자 화면용 전체 목록. 껐던 항목도 다시 켤 수 있도록 함께 보여준다. */
    List<CareTask> findAllByCaredOrderByScheduledTimeAscCareTaskIdAsc(Cared cared);

    /**
     * 켜져 있는 항목 전체. "오늘 것"을 고르는 판단은 CareTask.isScheduledOn()에서 한다.
     *
     * <p>반복 요일을 한 컬럼에 문자열로 담고 있어 SQL로 걸러내려면 LIKE 매칭이 필요한데, 한 사람의 할 일은
     * 많아야 십여 건이라 전부 읽어 메모리에서 거르는 편이 단순하고 틀릴 여지도 없다.
     */
    List<CareTask> findAllByCaredAndActiveIsTrueOrderByScheduledTimeAscCareTaskIdAsc(Cared cared);

    /** 회원 탈퇴 정리용. 딸린 완료 기록을 먼저 지워야 해서 목록으로 받는다. */
    List<CareTask> findAllByCaredIn(List<Cared> caredList);

    void deleteAllByCaredIn(List<Cared> caredList);
}
