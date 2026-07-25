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
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppWearPairingController {

    private final WearAuthService wearAuthService;

    /** 보호자 모바일에서 워치 연결 코드 발급 (cared_id를 이미 아는 경우). */
    @PostMapping("/cared/{caredId}/wear-pairing-codes")
    public ResponseEntity<WearPairingCodeResponse> issue(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wearAuthService.issuePairingCode(carerId, caredId));
    }

    /** 돌봄 대상자 등록 UI 없이 워치 연결만 쓰는 앱을 위한 발급. cared가 없으면 서버에서 자동 생성한다. */
    @PostMapping("/wear-pairing-codes")
    public ResponseEntity<WearPairingCodeResponse> issueAutoProvision(@CurrentCarerId Integer carerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wearAuthService.issuePairingCodeAutoProvision(carerId));
    }
}
