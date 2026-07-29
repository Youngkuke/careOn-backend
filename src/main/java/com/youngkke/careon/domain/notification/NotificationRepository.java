package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.policy.SavedPolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    void deleteAllBySavedPolicyIn(List<SavedPolicy> savedPolicies);

    /**
     * 알림 목록 (최신순). 저장 제도/제도까지 즉시 로딩한다.
     *
     * <p>cb 제도의 알림은 s.policy가 비어 있으므로 left join이어야 한다. 내부 조인이면 그 알림들이
     * 목록에서 통째로 빠지는데, 미읽음 개수(countUnreadByCarer)는 이 조인을 타지 않아 그대로 세어진다.
     * 그러면 종 뱃지에는 숫자가 뜨는데 목록에는 아무것도 없는 상태가 된다.
     */
    @Query("""
            select n from Notification n
            join fetch n.savedPolicy s
            left join fetch s.policy
            where s.carer = :carer
            order by n.sentAt desc
            """)
    List<Notification> findAllWithPolicyByCarer(@Param("carer") Carer carer);

    /** 미읽음 알림만 조회 (읽음 처리용). */
    @Query("select n from Notification n where n.savedPolicy.carer = :carer and n.read = false")
    List<Notification> findAllUnreadByCarer(@Param("carer") Carer carer);

    @Query("select count(n) from Notification n where n.savedPolicy.carer = :carer and n.read = false")
    long countUnreadByCarer(@Param("carer") Carer carer);

    boolean existsBySavedPolicyAndNotificationType(SavedPolicy savedPolicy, NotificationType notificationType);
}
