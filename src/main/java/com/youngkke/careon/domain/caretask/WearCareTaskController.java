package com.youngkke.careon.domain.caretask;

import com.youngkke.careon.domain.caretask.dto.CareTaskCompleteRequest;
import com.youngkke.careon.domain.caretask.dto.CareTaskCompleteResponse;
import com.youngkke.careon.domain.caretask.dto.CareTaskTodayResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear/care-tasks")
@RequiredArgsConstructor
public class WearCareTaskController {

    private final CareTaskService careTaskService;

    /** 오늘의 할 일 목록. 없으면 빈 배열. */
    @GetMapping("/today")
    public ResponseEntity<List<CareTaskTodayResponse>> listToday(@CurrentWearDeviceId Integer wearDeviceId) {
        return ResponseEntity.ok(careTaskService.listToday(wearDeviceId));
    }

    /** 오늘 몫을 체크하거나 해제한다. */
    @PatchMapping("/{taskId}")
    public ResponseEntity<CareTaskCompleteResponse> updateCompletion(
            @CurrentWearDeviceId Integer wearDeviceId,
            @PathVariable Integer taskId,
            @Valid @RequestBody CareTaskCompleteRequest request) {
        return ResponseEntity.ok(careTaskService.updateCompletion(wearDeviceId, taskId, request));
    }
}
