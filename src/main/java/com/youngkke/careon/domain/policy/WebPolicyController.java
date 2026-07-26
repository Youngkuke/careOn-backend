package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.AlternativePolicyResponse;
import com.youngkke.careon.domain.policy.dto.MatchedPolicyGroupResponse;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse;
import com.youngkke.careon.domain.policy.dto.PolicyListItemResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
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

    /** 제도 목록 조회 (검색/필터). 인증 불필요. */
    @GetMapping
    public ResponseEntity<List<PolicyListItemResponse>> getList(
            @RequestParam(required = false) String category,
            @RequestParam(name = "policy_type_ids", required = false) String policyTypeIds,
            @RequestParam(name = "agency_id", required = false) Integer agencyId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(policyService.getList(category, policyTypeIds, agencyId, keyword));
    }

    /**
     * 대안 복지 조회. 인증 불필요.
     * 자가진단 응답과 무관하게 고정 목록을 돌려주므로 조회 조건을 받지 않는다.
     * 구버전 프론트가 붙여 보내던 interest_policy_type_ids는 그냥 무시된다 (모르는 쿼리 파라미터라 400이 나지 않음).
     */
    @GetMapping("/alternatives")
    public ResponseEntity<List<AlternativePolicyResponse>> getAlternatives() {
        return ResponseEntity.ok(policyService.getAlternatives());
    }

    /** 맞춤 지원 제도 목록 조회 (챗봇 매칭 결과). 인증 필요. */
    @GetMapping("/matched")
    public ResponseEntity<List<MatchedPolicyGroupResponse>> getMatched(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(policyService.getMatched(carerId));
    }

    /** 제도 상세 조회. 인증 불필요. */
    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyDetailResponse> getDetail(@PathVariable Integer policyId) {
        return ResponseEntity.ok(policyService.getDetail(policyId));
    }
}
