package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.WasBenefitedRequest;
import com.youngkke.careon.domain.policy.dto.WasBenefitedResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/users/me/matched-policies")
@RequiredArgsConstructor
public class WebMatchedPolicyController {

    private final PolicyService policyService;

    /** 매칭 제도 수혜 여부 수정. */
    @PatchMapping("/{matchedPolicyId}")
    public ResponseEntity<WasBenefitedResponse> updateWasBenefited(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer matchedPolicyId,
            @Valid @RequestBody WasBenefitedRequest request) {
        return ResponseEntity.ok(policyService.updateWasBenefited(carerId, matchedPolicyId, request));
    }
}
