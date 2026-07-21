package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.dto.DocumentSummary;
import com.youngkke.careon.domain.policy.dto.AlternativePolicyResponse;
import com.youngkke.careon.domain.policy.dto.MatchedPolicyGroupResponse;
import com.youngkke.careon.domain.policy.dto.MatchedPolicyGroupResponse.MatchedPolicyItem;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse;
import com.youngkke.careon.domain.policy.dto.PolicyListItemResponse;
import com.youngkke.careon.domain.policy.dto.PolicyTypeSummary;
import com.youngkke.careon.domain.policy.dto.WasBenefitedRequest;
import com.youngkke.careon.domain.policy.dto.WasBenefitedResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private static final String MATCH_GROUP_SUITABLE = "적합";

    private final PolicyRepository policyRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final MatchedPolicyRepository matchedPolicyRepository;
    private final InterestPolicyTypeRepository interestPolicyTypeRepository;
    private final CarerRepository carerRepository;
    private final PolicySupport policySupport;

    /** 제도 목록 조회. 모든 조건은 선택이며, 조건이 없으면 전체 제도를 반환한다. */
    public List<PolicyListItemResponse> getList(
            String category, String policyTypeIds, Integer agencyId, String keyword) {
        PolicyCategory policyCategory = parseCategory(category);
        List<Integer> typeIds = parseIds(policyTypeIds);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        List<Policy> policies = policyRepository.search(
                policyCategory,
                agencyId,
                normalizedKeyword,
                !typeIds.isEmpty(),
                typeIds.isEmpty() ? List.of(-1) : typeIds);

        Map<Integer, List<PolicyTypeSummary>> typesByPolicy = policySupport.loadPolicyTypes(policies);
        return policies.stream()
                .map(policy -> new PolicyListItemResponse(
                        policy.getPolicyId(),
                        policy.getPolicyName(),
                        policy.getAgency().getAgencyId(),
                        policy.getAgency().getAgencyName(),
                        policy.getSummary(),
                        policy.getSupportPeriod(),
                        DateTimes.toIsoString(policy.getApplicationDeadline()),
                        toCategoryString(policy.getCategory()),
                        typesByPolicy.getOrDefault(policy.getPolicyId(), List.of())))
                .toList();
    }

    /** 대안 복지 조회. category는 항상 GENERAL로 고정하고, 관심 유형 ID로 필터링한다. */
    public List<AlternativePolicyResponse> getAlternatives(String interestPolicyTypeIds) {
        List<Integer> typeIds = parseIds(interestPolicyTypeIds);
        if (typeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_INTEREST_TYPE_IDS);
        }

        List<Policy> policies = policyRepository.search(PolicyCategory.GENERAL, null, null, true, typeIds);
        Map<Integer, List<PolicyTypeSummary>> typesByPolicy = policySupport.loadPolicyTypes(policies);

        return policies.stream()
                .map(policy -> new AlternativePolicyResponse(
                        policy.getPolicyId(),
                        policy.getPolicyName(),
                        policy.getAgency().getAgencyId(),
                        policy.getAgency().getAgencyName(),
                        policy.getSummary(),
                        policy.getSupportPeriod(),
                        DateTimes.toIsoString(policy.getApplicationDeadline()),
                        policy.getLink(),
                        toCategoryString(policy.getCategory()),
                        typesByPolicy.getOrDefault(policy.getPolicyId(), List.of())))
                .toList();
    }

    /** 제도 상세 조회. */
    public PolicyDetailResponse getDetail(Integer policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        List<DocumentSummary> documents = policySupport.loadDocuments(policy);
        List<PolicyTypeSummary> policyTypes = policySupport.loadPolicyTypes(policy);

        return new PolicyDetailResponse(
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getAgency().getAgencyId(),
                policy.getAgency().getAgencyName(),
                policy.getSupportPeriod(),
                policy.getCost(),
                policy.getSummary(),
                policy.getApplicationMethod(),
                policy.getDuration(),
                policy.getNotes(),
                policy.getDeadlineType(),
                policy.getDeadlineDateRaw(),
                DateTimes.toIsoString(policy.getApplicationDeadline()),
                policy.getResultNote(),
                policy.getLink(),
                policy.getContact(),
                policy.getApplicationRegion(),
                policy.getScheduleType(),
                policy.getAgeMin(),
                policy.getAgeMax(),
                policy.getExceptionAge(),
                policy.getIncomeCriteria(),
                policy.getQualificationText(),
                policy.getSupportTarget(),
                policy.getDuplicationRestriction(),
                policy.getOriginalNotice(),
                DateTimes.toIsoString(policy.getLastCheckedAt()),
                policy.getInfoReferenceYear(),
                policy.getIsLifetimeLimitOnce(),
                DateTimes.toIsoString(policy.getResultDate()),
                toCategoryString(policy.getCategory()),
                policyTypes,
                documents);
    }

    /**
     * 맞춤 지원 제도 목록 조회. 챗봇 매칭 결과를 유형별로 그룹핑한다.
     * 같은 제도가 여러 유형을 가져도 대표 유형 그룹 1곳에만 넣는다.
     * 대표 유형은 유저의 관심 유형 순서를 우선하고, 해당하지 않으면 가장 작은 policy_type_id를 쓴다.
     * 그룹 내 정렬은 "적합"을 먼저, "확인_불가"를 뒤에 둔다.
     */
    public List<MatchedPolicyGroupResponse> getMatched(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        List<MatchedPolicy> matches = matchedPolicyRepository.findAllWithPolicyByCarer(carer);
        if (matches.isEmpty()) {
            return List.of();
        }

        List<Policy> policies = matches.stream().map(MatchedPolicy::getPolicy).toList();
        Map<Integer, List<PolicyTypeSummary>> typesByPolicy = policySupport.loadPolicyTypes(policies);
        List<Integer> interestOrder = interestPolicyTypeRepository.findAllWithTypeByCarer(carer).stream()
                .map(interest -> interest.getPolicyType().getPolicyTypeId())
                .toList();

        // 대표 유형 기준으로 그룹핑
        Map<Integer, List<MatchedPolicyItem>> itemsByType = new LinkedHashMap<>();
        Map<Integer, String> typeNames = new LinkedHashMap<>();

        for (MatchedPolicy match : matches) {
            Policy policy = match.getPolicy();
            List<PolicyTypeSummary> types = typesByPolicy.getOrDefault(policy.getPolicyId(), List.of());
            if (types.isEmpty()) {
                continue; // 유형이 없는 제도는 그룹에 넣을 수 없으므로 제외
            }

            PolicyTypeSummary representative = chooseRepresentativeType(types, interestOrder);
            typeNames.putIfAbsent(representative.policyTypeId(), representative.typeName());
            itemsByType.computeIfAbsent(representative.policyTypeId(), key -> new ArrayList<>())
                    .add(new MatchedPolicyItem(
                            match.getMatchedPolicyId(),
                            policy.getPolicyId(),
                            match.getMatchGroup(),
                            match.getWasBenefited(),
                            policy.getPolicyName(),
                            policy.getAgency().getAgencyId(),
                            policy.getAgency().getAgencyName(),
                            policy.getSummary(),
                            policy.getSupportPeriod(),
                            DateTimes.toIsoString(policy.getApplicationDeadline()),
                            policy.getLink(),
                            types));
        }

        // 그룹 내 정렬: 적합 먼저, 그다음 확인_불가
        Comparator<MatchedPolicyItem> byMatchGroup = Comparator
                .comparingInt((MatchedPolicyItem item) -> MATCH_GROUP_SUITABLE.equals(item.matchGroup()) ? 0 : 1)
                .thenComparing(MatchedPolicyItem::policyId);

        return itemsByType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<MatchedPolicyItem> items = new ArrayList<>(entry.getValue());
                    items.sort(byMatchGroup);
                    return new MatchedPolicyGroupResponse(entry.getKey(), typeNames.get(entry.getKey()), items);
                })
                .toList();
    }

    /** 매칭 제도 수혜 여부 수정. */
    @Transactional
    public WasBenefitedResponse updateWasBenefited(
            Integer carerId, Integer matchedPolicyId, WasBenefitedRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        MatchedPolicy matchedPolicy = matchedPolicyRepository
                .findByMatchedPolicyIdAndCarer(matchedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCHED_POLICY_NOT_FOUND));

        matchedPolicy.updateWasBenefited(request.wasBenefited());

        return new WasBenefitedResponse(
                matchedPolicy.getMatchedPolicyId(), matchedPolicy.getWasBenefited(), "수혜 여부가 수정되었습니다.");
    }

    /** 유저 관심 유형 순서를 우선해 대표 유형을 고른다. 관심 유형에 없으면 가장 작은 policy_type_id. */
    private PolicyTypeSummary chooseRepresentativeType(List<PolicyTypeSummary> types, List<Integer> interestOrder) {
        for (Integer interestTypeId : interestOrder) {
            for (PolicyTypeSummary type : types) {
                if (type.policyTypeId().equals(interestTypeId)) {
                    return type;
                }
            }
        }
        return types.stream()
                .min(Comparator.comparing(PolicyTypeSummary::policyTypeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_TYPE_NOT_FOUND));
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private PolicyCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PolicyCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String toCategoryString(PolicyCategory category) {
        return category == null ? null : category.name();
    }

    private List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
