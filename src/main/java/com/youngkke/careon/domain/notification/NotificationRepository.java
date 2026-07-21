package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    void deleteAllBySavedPolicyIn(List<SavedPolicy> savedPolicies);

    List<Notification> findAllBySavedPolicy_CarerOrderBySentAtDesc(Carer carer);

    boolean existsBySavedPolicyAndNotificationType(SavedPolicy savedPolicy, NotificationType notificationType);
}
