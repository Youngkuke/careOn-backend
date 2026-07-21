package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/saved-policies")
@RequiredArgsConstructor
public class AppSavedPolicyController {

    private final SavedPolicyService savedPolicyService;

    @GetMapping
    public ResponseEntity<List<AppSavedPolicyResponse>> getList(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(savedPolicyService.getAppList(userId));
    }
}
