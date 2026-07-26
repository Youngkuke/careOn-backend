package com.youngkke.careon.domain.timeline;

import com.youngkke.careon.domain.timeline.dto.CareTimelineItemResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cared/{caredId}")
@RequiredArgsConstructor
public class AppCareTimelineController {

    private final CareTimelineService careTimelineService;

    /** Care 타임라인. 최신순, next_cursor가 null이면 마지막 페이지. */
    @GetMapping("/care-timeline")
    public ResponseEntity<CursorPageResponse<CareTimelineItemResponse>> list(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(careTimelineService.list(carerId, caredId, cursor, limit));
    }
}
