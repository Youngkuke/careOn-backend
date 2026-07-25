package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.WearPairRequest;
import com.youngkke.careon.domain.wear.dto.WearPairResponse;
import com.youngkke.careon.domain.wear.dto.WearRefreshRequest;
import com.youngkke.careon.domain.wear.dto.WearRefreshResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear/auth")
@RequiredArgsConstructor
public class WearAuthController {

    private final WearAuthService wearAuthService;

    /** 워치에서 6자리 연결 코드 입력. */
    @PostMapping("/pair")
    public ResponseEntity<WearPairResponse> pair(@Valid @RequestBody WearPairRequest request) {
        return ResponseEntity.ok(wearAuthService.pair(request));
    }

    /** 워치 access token 갱신. */
    @PostMapping("/refresh")
    public ResponseEntity<WearRefreshResponse> refresh(@Valid @RequestBody WearRefreshRequest request) {
        return ResponseEntity.ok(wearAuthService.refresh(request));
    }
}
