package com.youngkke.careon.domain.carer;

import com.youngkke.careon.domain.carer.dto.AppLoginResponse;
import com.youngkke.careon.domain.carer.dto.AppMeResponse;
import com.youngkke.careon.domain.carer.dto.LoginRequest;
import com.youngkke.careon.domain.carer.dto.RefreshTokenRequest;
import com.youngkke.careon.domain.carer.dto.RefreshTokenResponse;
import com.youngkke.careon.domain.carer.dto.UpdateAppProfileRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users")
@RequiredArgsConstructor
public class AppCarerController {

    private final CarerService carerService;

    @PostMapping("/login")
    public ResponseEntity<AppLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(carerService.loginApp(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(carerService.logout(userId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(carerService.reissueToken(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AppMeResponse> me(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(carerService.getAppMe(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<MessageResponse> updateMe(
            @CurrentCarerId Integer userId, @Valid @RequestBody UpdateAppProfileRequest request) {
        return ResponseEntity.ok(carerService.updateAppProfile(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> withdraw(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(carerService.withdraw(userId));
    }
}
