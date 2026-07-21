package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.AlternativePolicyResponse;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/policies")
@RequiredArgsConstructor
public class WebPolicyController {

    private final PolicyService policyService;

    @GetMapping("/alternatives")
    public ResponseEntity<List<AlternativePolicyResponse>> getAlternatives(
            @RequestParam(required = false) String interestTypeIds) {
        return ResponseEntity.ok(policyService.getAlternatives(interestTypeIds));
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyDetailResponse> getDetail(@PathVariable Integer policyId) {
        return ResponseEntity.ok(policyService.getDetail(policyId));
    }
}
