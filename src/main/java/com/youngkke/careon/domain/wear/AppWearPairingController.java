package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.WearPairingCodeResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cared/{caredId}/wear-pairing-codes")
@RequiredArgsConstructor
public class AppWearPairingController {

    private final WearAuthService wearAuthService;

    /** 보호자 모바일에서 워치 연결 코드 발급. */
    @PostMapping
    public ResponseEntity<WearPairingCodeResponse> issue(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wearAuthService.issuePairingCode(carerId, caredId));
    }
}
