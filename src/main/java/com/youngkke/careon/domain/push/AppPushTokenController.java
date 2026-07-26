package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.push.dto.PushTokenDeleteRequest;
import com.youngkke.careon.domain.push.dto.PushTokenRegisterRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/push-tokens")
@RequiredArgsConstructor
public class AppPushTokenController {

    private final PushTokenService pushTokenService;

    /** 앱이 발급받은 Expo 푸시 토큰을 등록한다. */
    @PutMapping
    public ResponseEntity<Void> register(
            @CurrentCarerId Integer userId, @Valid @RequestBody PushTokenRegisterRequest request) {
        pushTokenService.register(userId, request);
        return ResponseEntity.noContent().build();
    }

    /** 로그아웃 등으로 더 이상 푸시를 받지 않을 토큰을 해제한다. */
    @DeleteMapping
    public ResponseEntity<Void> unregister(
            @CurrentCarerId Integer userId, @Valid @RequestBody PushTokenDeleteRequest request) {
        pushTokenService.unregister(userId, request);
        return ResponseEntity.noContent().build();
    }
}
