package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.LiveLocationUpdateRequest;
import com.youngkke.careon.domain.wear.dto.LiveLocationUpdateResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear")
@RequiredArgsConstructor
public class WearLiveLocationController {

    private final WearLiveLocationService wearLiveLocationService;

    /** 워치가 최신 위치를 전송. 추적이 꺼져 있으면 저장하지 않고 accepted=false를 반환한다. */
    @PostMapping("/live-location")
    public ResponseEntity<LiveLocationUpdateResponse> update(
            @CurrentWearDeviceId Integer wearDeviceId, @Valid @RequestBody LiveLocationUpdateRequest request) {
        return ResponseEntity.ok(wearLiveLocationService.updateLiveLocation(wearDeviceId, request));
    }
}
