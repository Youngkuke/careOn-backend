package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.policy.SavedPolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    void deleteAllBySavedPolicyIn(List<SavedPolicy> savedPolicies);

    /** 알림 목록 (최신순). 저장 제도/제도까지 즉시 로딩한다. */
    @Query("""
            select n from Notification n
            join fetch n.savedPolicy s
            join fetch s.policy
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
