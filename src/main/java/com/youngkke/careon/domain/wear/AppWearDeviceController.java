package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.LiveLocationResponse;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingRequest;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingResponse;
import com.youngkke.careon.domain.wear.dto.WearDeviceStatusResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppWearDeviceController {

    private final WearLiveLocationService wearLiveLocationService;
    private final WearAuthService wearAuthService;

    /** 워치 실제 연결 상태 조회 ("코드 발급됨"과 "연결 완료"를 구분). 연결된 적 없거나 해제됐으면 204. */
    @GetMapping("/wear-device")
    public ResponseEntity<WearDeviceStatusResponse> getDeviceStatus(@CurrentCarerId Integer carerId) {
        WearDeviceStatusResponse response = wearLiveLocationService.getDeviceStatus(carerId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /** 워치 연결 해제. 이미 해제됐거나 연결된 워치가 없어도 성공으로 답한다(멱등). */
    @DeleteMapping("/wear-device")
    public ResponseEntity<MessageResponse> disconnect(@CurrentCarerId Integer carerId) {
        wearAuthService.disconnect(carerId);
        return ResponseEntity.ok(new MessageResponse("워치 연결을 해제했어요."));
    }

    /** 실시간 위치 공유 시작/중지. */
    @PatchMapping("/wear/live-location/tracking")
    public ResponseEntity<LiveLocationTrackingResponse> setTracking(
            @CurrentCarerId Integer carerId, @Valid @RequestBody LiveLocationTrackingRequest request) {
        return ResponseEntity.ok(wearLiveLocationService.setTracking(carerId, request));
    }

    /** 최신 위치 조회. 없으면 204. */
    @GetMapping("/wear/live-location")
    public ResponseEntity<LiveLocationResponse> getLiveLocation(@CurrentCarerId Integer carerId) {
        LiveLocationResponse response = wearLiveLocationService.getLiveLocation(carerId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
