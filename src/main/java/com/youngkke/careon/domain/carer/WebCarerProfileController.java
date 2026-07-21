package com.youngkke.careon.domain.carer;

import com.youngkke.careon.domain.carer.dto.CaredCreateResponse;
import com.youngkke.careon.domain.carer.dto.CaredRequest;
import com.youngkke.careon.domain.carer.dto.DiagnosisProfileRequest;
import com.youngkke.careon.domain.carer.dto.DiagnosisProfileResponse;
import com.youngkke.careon.domain.carer.dto.IncomeSignalResponse;
import com.youngkke.careon.domain.carer.dto.IncomeSignalUpdateRequest;
import com.youngkke.careon.domain.carer.dto.IncomeSignalUpdateResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 진단 프로필 관리 API (진단 프로필, 돌봄 대상자, 소득 추론 근거). */
@RestController
@RequestMapping("/api/web/users/me")
@RequiredArgsConstructor
public class WebCarerProfileController {

    private final CarerProfileService carerProfileService;

    @GetMapping("/diagnosis-profile")
    public ResponseEntity<DiagnosisProfileResponse> getDiagnosisProfile(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(carerProfileService.getDiagnosisProfile(carerId));
    }

    @PatchMapping("/diagnosis-profile")
    public ResponseEntity<MessageResponse> updateDiagnosisProfile(
            @CurrentCarerId Integer carerId, @Valid @RequestBody DiagnosisProfileRequest request) {
        return ResponseEntity.ok(carerProfileService.updateDiagnosisProfile(carerId, request));
    }

    @PostMapping("/cared")
    public ResponseEntity<CaredCreateResponse> addCared(
            @CurrentCarerId Integer carerId, @Valid @RequestBody CaredRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carerProfileService.addCared(carerId, request));
    }

    @PatchMapping("/cared/{caredId}")
    public ResponseEntity<MessageResponse> updateCared(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @Valid @RequestBody CaredRequest request) {
        return ResponseEntity.ok(carerProfileService.updateCared(carerId, caredId, request));
    }

    @DeleteMapping("/cared/{caredId}")
    public ResponseEntity<MessageResponse> deleteCared(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        return ResponseEntity.ok(carerProfileService.deleteCared(carerId, caredId));
    }

    @GetMapping("/income-signals")
    public ResponseEntity<List<IncomeSignalResponse>> getIncomeSignals(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(carerProfileService.getIncomeSignals(carerId));
    }

    @PatchMapping("/income-signals/{signalId}")
    public ResponseEntity<IncomeSignalUpdateResponse> updateIncomeSignal(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer signalId,
            @Valid @RequestBody IncomeSignalUpdateRequest request) {
        return ResponseEntity.ok(carerProfileService.updateIncomeSignal(carerId, signalId, request));
    }
}
