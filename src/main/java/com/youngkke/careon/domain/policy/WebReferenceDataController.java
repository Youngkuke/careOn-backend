package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.document.dto.DocumentDetailResponse;
import com.youngkke.careon.domain.document.dto.DocumentSummary;
import com.youngkke.careon.domain.policy.dto.AgencyResponse;
import com.youngkke.careon.domain.policy.dto.PolicyTypeSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 기준 데이터 관리 API. 모두 인증 불필요. */
@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/agencies")
    public ResponseEntity<List<AgencyResponse>> getAgencies() {
        return ResponseEntity.ok(referenceDataService.getAgencies());
    }

    @GetMapping("/agencies/{agencyId}")
    public ResponseEntity<AgencyResponse> getAgency(@PathVariable Integer agencyId) {
        return ResponseEntity.ok(referenceDataService.getAgency(agencyId));
    }

    @GetMapping("/policy-types")
    public ResponseEntity<List<PolicyTypeSummary>> getPolicyTypes() {
        return ResponseEntity.ok(referenceDataService.getPolicyTypes());
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentSummary>> getDocuments() {
        return ResponseEntity.ok(referenceDataService.getDocuments());
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocument(@PathVariable Integer documentId) {
        return ResponseEntity.ok(referenceDataService.getDocument(documentId));
    }
}
