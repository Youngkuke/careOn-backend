package com.youngkke.careon.domain.carer;

import com.youngkke.careon.domain.carer.dto.AppInstallStatusRequest;
import com.youngkke.careon.domain.carer.dto.LoginRequest;
import com.youngkke.careon.domain.carer.dto.PasswordResetLinkRequest;
import com.youngkke.careon.domain.carer.dto.PasswordResetRequest;
import com.youngkke.careon.domain.carer.dto.SignupRequest;
import com.youngkke.careon.domain.carer.dto.SignupResponse;
import com.youngkke.careon.domain.carer.dto.UpdateWebProfileRequest;
import com.youngkke.careon.domain.carer.dto.WebLoginResponse;
import com.youngkke.careon.domain.carer.dto.WebMeResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/users")
@RequiredArgsConstructor
public class WebCarerController {

    private final CarerService carerService;

    @PostMapping("/register")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = carerService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<WebLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(carerService.loginWeb(request));
    }

    @GetMapping("/me")
    public ResponseEntity<WebMeResponse> me(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(carerService.getWebMe(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<MessageResponse> updateMe(
            @CurrentCarerId Integer userId, @Valid @RequestBody UpdateWebProfileRequest request) {
        return ResponseEntity.ok(carerService.updateWebProfile(userId, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> withdraw(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(carerService.withdraw(userId));
    }

    @PatchMapping("/me/app-install-status")
    public ResponseEntity<MessageResponse> updateAppInstallStatus(
            @CurrentCarerId Integer userId, @Valid @RequestBody AppInstallStatusRequest request) {
        return ResponseEntity.ok(carerService.updateAppInstallStatus(userId, request));
    }

    @PostMapping("/password/reset-link")
    public ResponseEntity<MessageResponse> sendPasswordResetLink(
            @Valid @RequestBody PasswordResetLinkRequest request) {
        return ResponseEntity.ok(carerService.sendPasswordResetLink(request));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(carerService.resetPassword(request));
    }
}
