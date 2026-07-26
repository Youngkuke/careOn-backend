package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.document.dto.DocumentSummary;
import java.util.List;

/**
 * 저장한 제도 목록 조회(웹) 응답 항목.
 * 카드 표시/상세 진입 정보에 더해, 모바일에서 신청 완료한 제도를 웹에서 구분하고 수혜 여부를
 * 입력할 수 있도록 신청/수혜 상태를 함께 반환한다. (is_checked 서류 체크 여부는 앱 전용이라 제외)
 *
 * <p>찜 대상이 두 갈래라 policyId와 servId 중 하나만 값이 있다. cb 제도는 제도 유형·필요 서류 데이터가
 * 아직 없어 policyTypes/documents가 빈 배열이고, 마감일·발표일도 내려가지 않는다.
 *
 * @param servId cb(복지로) 제도를 찜한 경우의 식별자. 기존 제도면 null.
 * @param isActive 원본에서 아직 운영 중인 제도인지. cb 제도만 의미가 있고, 기존 제도는 항상 true다.
 * @param regionName cb 제도의 시행 지역 (예: "서울특별시 강남구"). 기존 제도면 null.
 */
public record SavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        String servId,
        boolean isActive,
        Integer matchedPolicyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String summary,
        String supportPeriod,
        String applicationDeadline,
        boolean isApplied,
        String applicationStatus,
        String appliedAt,
        String resultDate,
        String benefitStatus,
        String benefitCheckedAt,
        String link,
        List<PolicyTypeSummary> policyTypes,
        List<DocumentSummary> documents,
        String regionName) {}
