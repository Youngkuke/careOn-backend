package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyAppliedRequest;
import com.youngkke.careon.domain.policy.dto.SavedPolicyAppliedResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/users/me/saved-policies")
@RequiredArgsConstructor
public class AppSavedPolicyController {

    private final SavedPolicyService savedPolicyService;

    /** 저장한 제도 목록 조회 (캘린더용). */
    @GetMapping
    public ResponseEntity<List<AppSavedPolicyResponse>> getList(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(savedPolicyService.getAppList(carerId));
    }

    /** 제도 저장 취소. 투두리스트에서 마감된 제도에 "아니오"로 응답할 때도 사용한다. */
    @DeleteMapping("/{savedPolicyId}")
    public ResponseEntity<MessageResponse> cancel(
            @CurrentCarerId Integer carerId, @PathVariable Integer savedPolicyId) {
        return ResponseEntity.ok(savedPolicyService.cancel(carerId, savedPolicyId));
    }

    /** 신청 여부 응답. 마감 지난 제도에 "예"로 응답할 때 호출한다. */
    @PostMapping("/{savedPolicyId}/applied")
    public ResponseEntity<SavedPolicyAppliedResponse> markApplied(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer savedPolicyId,
            @Valid @RequestBody SavedPolicyAppliedRequest request) {
        return ResponseEntity.ok(savedPolicyService.markApplied(carerId, savedPolicyId));
    }
}
