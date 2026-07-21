package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.document.Document;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.policy.dto.AlternativePolicyResponse;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse.DocumentDetail;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse.IssuerDetail;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final ConnectPolicyDocumentRepository connectPolicyDocumentRepository;
    private final DocumentIssueRepository documentIssueRepository;

    /** 대안 복지 조회. targetCategory는 항상 GENERAL로 고정하고, interestTypeIds(policyTypeId 목록)로 필터링한다. */
    public List<AlternativePolicyResponse> getAlternatives(String interestTypeIds) {
        List<Integer> policyTypeIds = parseIds(interestTypeIds);
        if (policyTypeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_INTEREST_TYPE_IDS);
        }

        return policyRepository.findByCategoryAndPolicyType_PolicyTypeIdIn(PolicyCategory.GENERAL, policyTypeIds)
                .stream()
                .map(AlternativePolicyResponse::from)
                .toList();
    }

    /** 제도 상세 조회. */
    public PolicyDetailResponse getDetail(Integer policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        List<DocumentDetail> documents = connectPolicyDocumentRepository.findByPolicy(policy).stream()
                .map(ConnectPolicyDocument::getDocument)
                .map(this::toDocumentDetail)
                .toList();

        return new PolicyDetailResponse(
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getPolicyType().getTypeName(),
                policy.getAgency().getAgencyName(),
                policy.getSupportPeriod(),
                policy.getCost(),
                toDateString(policy.getApplicationDeadline()),
                policy.getApplicationMethod(),
                policy.getDuration(),
                toDateString(policy.getResultDate()),
                policy.getContact(),
                documents);
    }

    private DocumentDetail toDocumentDetail(Document document) {
        List<IssuerDetail> issuers = documentIssueRepository.findByDocument(document).stream()
                .map(issue -> new IssuerDetail(
                        issue.getDocumentIssuer().getIssuerName(), issue.getDocumentIssuer().getIssuerSite()))
                .toList();
        return new DocumentDetail(document.getDocumentId(), document.getDocumentName(), issuers);
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().toString() : null;
    }

    private List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return java.util.Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
