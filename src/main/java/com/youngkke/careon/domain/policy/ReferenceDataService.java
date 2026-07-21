package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.document.Document;
import com.youngkke.careon.domain.document.DocumentRepository;
import com.youngkke.careon.domain.document.dto.DocumentDetailResponse;
import com.youngkke.careon.domain.document.dto.DocumentDetailResponse.DocumentPolicyItem;
import com.youngkke.careon.domain.document.dto.DocumentSummary;
import com.youngkke.careon.domain.document.dto.IssuerSummary;
import com.youngkke.careon.domain.policy.dto.AgencyResponse;
import com.youngkke.careon.domain.policy.dto.AgencyResponse.AgencyPolicyItem;
import com.youngkke.careon.domain.policy.dto.PolicyTypeSummary;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기준 데이터(기관/제도유형/서류) 조회. 인증 없이 사용 가능한 읽기 전용 API들. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final AgencyRepository agencyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final DocumentRepository documentRepository;
    private final PolicyRepository policyRepository;
    private final ConnectPolicyDocumentRepository connectPolicyDocumentRepository;
    private final PolicySupport policySupport;

    /** 기관 목록 조회. */
    public List<AgencyResponse> getAgencies() {
        return agencyRepository.findAll().stream()
                .sorted((a, b) -> a.getAgencyId().compareTo(b.getAgencyId()))
                .map(agency -> AgencyResponse.ofSummary(agency.getAgencyId(), agency.getAgencyName()))
                .toList();
    }

    /** 기관 상세 조회 (연결된 제도 목록 포함). */
    public AgencyResponse getAgency(Integer agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENCY_NOT_FOUND));

        List<AgencyPolicyItem> policies = policyRepository.findAllByAgency_AgencyIdOrderByPolicyIdAsc(agencyId).stream()
                .map(policy -> new AgencyPolicyItem(policy.getPolicyId(), policy.getPolicyName()))
                .toList();

        return new AgencyResponse(agency.getAgencyId(), agency.getAgencyName(), policies);
    }

    /** 제도 유형 목록 조회. */
    public List<PolicyTypeSummary> getPolicyTypes() {
        return policyTypeRepository.findAll().stream()
                .sorted((a, b) -> a.getPolicyTypeId().compareTo(b.getPolicyTypeId()))
                .map(PolicyTypeSummary::from)
                .toList();
    }

    /** 서류 목록 조회 (발급처 포함). */
    public List<DocumentSummary> getDocuments() {
        List<Document> documents = documentRepository.findAll().stream()
                .sorted((a, b) -> a.getDocumentId().compareTo(b.getDocumentId()))
                .toList();
        return policySupport.toDocumentSummaries(documents);
    }

    /** 서류 상세 조회 (발급처 + 이 서류를 요구하는 제도 목록). */
    public DocumentDetailResponse getDocument(Integer documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        List<IssuerSummary> issuers = policySupport.toDocumentSummaries(List.of(document)).stream()
                .findFirst()
                .map(DocumentSummary::issuers)
                .orElse(List.of());

        List<DocumentPolicyItem> policies =
                connectPolicyDocumentRepository.findAllWithPolicyByDocument(document).stream()
                        .map(ConnectPolicyDocument::getPolicy)
                        .map(policy -> new DocumentPolicyItem(policy.getPolicyId(), policy.getPolicyName()))
                        .toList();

        return new DocumentDetailResponse(
                document.getDocumentId(), document.getDocumentName(), issuers, policies);
    }
}
