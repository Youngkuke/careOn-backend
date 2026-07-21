package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeResponse;
import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeUpdateRequest;
import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeUpdateResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/users/me/interest-policy-types")
@RequiredArgsConstructor
public class WebInterestPolicyTypeController {

    private final InterestPolicyTypeService interestPolicyTypeService;

    @GetMapping
    public ResponseEntity<List<InterestPolicyTypeResponse>> getMyInterestTypes(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(interestPolicyTypeService.getMyInterestTypes(carerId));
    }

    @PatchMapping
    public ResponseEntity<InterestPolicyTypeUpdateResponse> updateMyInterestTypes(
            @CurrentCarerId Integer carerId, @Valid @RequestBody InterestPolicyTypeUpdateRequest request) {
        return ResponseEntity.ok(interestPolicyTypeService.updateMyInterestTypes(carerId, request));
    }
}
