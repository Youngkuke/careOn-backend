package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.notification.dto.NotificationResponse;
import com.youngkke.careon.domain.notification.dto.ReadAllResponse;
import com.youngkke.careon.domain.notification.dto.UnreadCountResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final NotificationService notificationService;

    /** 알림 목록 조회 (최신순). 읽음 처리는 하지 않는다. */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getList(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(notificationService.getList(carerId));
    }

    /** 미읽음 알림 개수 조회 (종 아이콘 뱃지용). */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(carerId));
    }

    /** 모든 알림 읽음 처리. */
    @PatchMapping("/read-all")
    public ResponseEntity<ReadAllResponse> readAll(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(notificationService.readAll(carerId));
    }
}
