package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.document.dto.DocumentSummary;
import java.util.List;

/** 제도 상세 조회 응답. (external_ref는 내부 참조용이라 응답에 포함하지 않는다) */
public record PolicyDetailResponse(
        Integer policyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String supportPeriod,
        String cost,
        String summary,
        String applicationMethod,
        String duration,
        String notes,
        String deadlineType,
        String deadlineDateRaw,
        String applicationDeadline,
        String resultNote,
        String link,
        String contact,
        String applicationRegion,
        String scheduleType,
        Integer ageMin,
        Integer ageMax,
        String exceptionAge,
        String incomeCriteria,
        String qualificationText,
        String supportTarget,
        String duplicationRestriction,
        String originalNotice,
        String lastCheckedAt,
        Integer infoReferenceYear,
        Boolean isLifetimeLimitOnce,
        String resultDate,
        String category,
        List<PolicyTypeSummary> policyTypes,
        List<DocumentSummary> documents) {}
