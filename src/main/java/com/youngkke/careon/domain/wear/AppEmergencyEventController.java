package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.ActiveEmergencyEventResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyEventAcknowledgeResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppEmergencyEventController {

    private final EmergencyEventService emergencyEventService;

    /** SOS 이력 목록. 최신순, next_cursor가 null이면 마지막 페이지. */
    @GetMapping("/cared/{caredId}/emergency-events")
    public ResponseEntity<CursorPageResponse<ActiveEmergencyEventResponse>> listHistory(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(emergencyEventService.listHistory(carerId, caredId, cursor, limit));
    }

    /** 보호자 모바일에서 현재 활성 SOS 조회. 없으면 204. */
    @GetMapping("/cared/{caredId}/emergency-events/active")
    public ResponseEntity<ActiveEmergencyEventResponse> getActive(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        ActiveEmergencyEventResponse response = emergencyEventService.getActive(carerId, caredId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /**
     * 이벤트 ID로 SOS 1건 조회. 푸시 딥링크로 진입했을 때 쓴다.
     * 확인 완료된 건도 조회되며, 남의 이벤트나 없는 이벤트는 모두 404다.
     */
    @GetMapping("/emergency-events/{eventId}")
    public ResponseEntity<ActiveEmergencyEventResponse> getById(
            @CurrentCarerId Integer carerId, @PathVariable Integer eventId) {
        return ResponseEntity.ok(emergencyEventService.getById(carerId, eventId));
    }

    /** 보호자가 "확인했어요" 처리. */
    @PatchMapping("/emergency-events/{eventId}/acknowledge")
    public ResponseEntity<EmergencyEventAcknowledgeResponse> acknowledge(
            @CurrentCarerId Integer carerId, @PathVariable Integer eventId) {
        return ResponseEntity.ok(emergencyEventService.acknowledge(carerId, eventId));
    }
}
