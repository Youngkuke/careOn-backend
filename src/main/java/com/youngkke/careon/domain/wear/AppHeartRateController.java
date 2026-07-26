package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.LatestHeartRateResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cared/{caredId}")
@RequiredArgsConstructor
public class AppHeartRateController {

    private final HeartRateService heartRateService;

    /** 최신 심박수 조회. 측정 기록이 없으면 204. */
    @GetMapping("/heart-rates/latest")
    public ResponseEntity<LatestHeartRateResponse> getLatest(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        LatestHeartRateResponse response = heartRateService.getLatest(carerId, caredId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
