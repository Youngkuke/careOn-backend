package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.WearDeviceStatusReportRequest;
import com.youngkke.careon.domain.wear.dto.WearDeviceStatusReportResponse;
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
public class WearDeviceStatusController {

    private final WearDeviceStatusService wearDeviceStatusService;

    /** 워치가 배터리 등 기기 상태를 주기적으로 보고한다. */
    @PostMapping("/device-status")
    public ResponseEntity<WearDeviceStatusReportResponse> report(
            @CurrentWearDeviceId Integer wearDeviceId, @Valid @RequestBody WearDeviceStatusReportRequest request) {
        return ResponseEntity.ok(wearDeviceStatusService.report(wearDeviceId, request));
    }
}
