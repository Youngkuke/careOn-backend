package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyAppliedRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 마감 지난 저장 제도에 대해 "신청했어요(예)"를 기록한다. (아니오는 기존 저장취소 API 사용) */
    @PatchMapping("/{savedPolicyId}/applied")
    public ResponseEntity<MessageResponse> markApplied(
            @CurrentCarerId Integer userId,
            @PathVariable Integer savedPolicyId,
            @Valid @RequestBody SavedPolicyAppliedRequest request) {
        return ResponseEntity.ok(savedPolicyService.markApplied(userId, savedPolicyId));
    }
}
