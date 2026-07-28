package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.WearConnectionResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 워치가 자기 연결 상태를 확인하고 직접 끊는 API. 보호자 모바일 쪽 해제는 AppWearDeviceController에 따로 있다. */
@RestController
@RequestMapping("/api/wear/connection")
@RequiredArgsConstructor
public class WearConnectionController {

    private final WearAuthService wearAuthService;

    /** 워치 설정 화면에 보여줄 연결 정보(기기 + 보호자) 조회. */
    @GetMapping
    public ResponseEntity<WearConnectionResponse> getConnection(@CurrentWearDeviceId Integer wearDeviceId) {
        return ResponseEntity.ok(wearAuthService.getConnection(wearDeviceId));
    }

    /** 워치에서 연결 해제. 이 호출로 워치 자신의 토큰이 무효화되므로 재시도하면 401이다. */
    @DeleteMapping
    public ResponseEntity<Void> disconnect(@CurrentWearDeviceId Integer wearDeviceId) {
        wearAuthService.disconnectFromWear(wearDeviceId);
        return ResponseEntity.noContent().build();
    }
}
