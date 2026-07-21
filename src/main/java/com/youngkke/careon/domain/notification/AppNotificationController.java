package com.youngkke.careon.domain.notification;

import com.youngkke.careon.domain.notification.dto.NotificationResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getList(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(notificationService.getList(userId));
    }
}
