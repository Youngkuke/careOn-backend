package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.document.Document;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.document.dto.DocumentSummary;
import com.youngkke.careon.domain.document.dto.IssuerSummary;
import com.youngkke.careon.domain.policy.dto.PolicyTypeSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 제도의 유형/서류를 묶어서 조회하는 공통 로직. 여러 서비스에서 재사용한다. */
@Component
@RequiredArgsConstructor
public class PolicySupport {

    private final ConnectPolicyPolicyTypeRepository connectPolicyPolicyTypeRepository;
    private final ConnectPolicyDocumentRepository connectPolicyDocumentRepository;
    private final DocumentIssueRepository documentIssueRepository;

    /** 여러 제도의 유형 목록을 한 번에 조회한다. (policyId -> 유형 목록) */
    public Map<Integer, List<PolicyTypeSummary>> loadPolicyTypes(List<Policy> policies) {
        Map<Integer, List<PolicyTypeSummary>> result = new HashMap<>();
        if (policies.isEmpty()) {
            return result;
        }

        for (ConnectPolicyPolicyType connect : connectPolicyPolicyTypeRepository.findAllWithTypeByPolicyIn(policies)) {
            result.computeIfAbsent(connect.getPolicy().getPolicyId(), key -> new ArrayList<>())
                    .add(PolicyTypeSummary.from(connect.getPolicyType()));
        }
        result.values().forEach(list -> list.sort(Comparator.comparing(PolicyTypeSummary::policyTypeId)));
        return result;
    }

    /** 제도 하나의 유형 목록. */
    public List<PolicyTypeSummary> loadPolicyTypes(Policy policy) {
        return loadPolicyTypes(List.of(policy)).getOrDefault(policy.getPolicyId(), List.of());
    }

    /** 제도 하나의 필요 서류 목록 (발급처 포함). */
    public List<DocumentSummary> loadDocuments(Policy policy) {
        List<Document> documents = connectPolicyDocumentRepository.findByPolicy(policy).stream()
                .map(ConnectPolicyDocument::getDocument)
                .toList();
        return toDocumentSummaries(documents);
    }

    /** 서류 목록을 발급처까지 채워서 변환한다. */
    public List<DocumentSummary> toDocumentSummaries(List<Document> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<IssuerSummary>> issuersByDocument = new HashMap<>();
        documentIssueRepository.findAllWithIssuerByDocumentIn(documents).forEach(issue ->
                issuersByDocument.computeIfAbsent(issue.getDocument().getDocumentId(), key -> new ArrayList<>())
                        .add(IssuerSummary.from(issue.getDocumentIssuer())));

        return documents.stream()
                .map(document -> new DocumentSummary(
                        document.getDocumentId(),
                        document.getDocumentName(),
                        issuersByDocument.getOrDefault(document.getDocumentId(), List.of())))
                .toList();
    }
}
