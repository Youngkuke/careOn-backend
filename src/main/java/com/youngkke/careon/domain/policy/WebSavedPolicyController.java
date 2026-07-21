package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.policy.dto.SavePolicyRequest;
import com.youngkke.careon.domain.policy.dto.SavePolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyResponse;
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
@RequestMapping("/api/web/users/me/saved-policies")
@RequiredArgsConstructor
public class WebSavedPolicyController {

    private final SavedPolicyService savedPolicyService;

    @GetMapping
    public ResponseEntity<List<SavedPolicyResponse>> getList(@CurrentCarerId Integer userId) {
        return ResponseEntity.ok(savedPolicyService.getWebList(userId));
    }

    @PostMapping
    public ResponseEntity<SavePolicyResponse> save(
            @CurrentCarerId Integer userId, @Valid @RequestBody SavePolicyRequest request) {
        return ResponseEntity.ok(savedPolicyService.save(userId, request));
    }

    @DeleteMapping("/{savedPolicyId}")
    public ResponseEntity<MessageResponse> cancel(
            @CurrentCarerId Integer userId, @PathVariable Integer savedPolicyId) {
        return ResponseEntity.ok(savedPolicyService.cancel(userId, savedPolicyId));
    }
}
