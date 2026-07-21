package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeResponse;
import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeUpdateRequest;
import com.youngkke.careon.domain.policy.dto.InterestPolicyTypeUpdateResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저의 관심 제도 유형 조회/수정. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestPolicyTypeService {

    private final InterestPolicyTypeRepository interestPolicyTypeRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final CarerRepository carerRepository;

    /** 내 관심 유형 조회. */
    public List<InterestPolicyTypeResponse> getMyInterestTypes(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return interestPolicyTypeRepository.findAllWithTypeByCarer(carer).stream()
                .map(interest -> new InterestPolicyTypeResponse(
                        interest.getInterestPolicyTypeId(),
                        interest.getPolicyType().getPolicyTypeId(),
                        interest.getPolicyType().getTypeName()))
                .toList();
    }

    /** 내 관심 유형 수정 (전체 교체). */
    @Transactional
    public InterestPolicyTypeUpdateResponse updateMyInterestTypes(
            Integer carerId, InterestPolicyTypeUpdateRequest request) {
        Carer carer = getCarerOrThrow(carerId);

        List<Integer> typeIds = request.interestPolicyTypeIds();
        if (typeIds == null || typeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_INTEREST_TYPE_SELECTION);
        }

        List<PolicyType> policyTypes = typeIds.stream()
                .map(id -> policyTypeRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_TYPE_NOT_FOUND)))
                .toList();

        interestPolicyTypeRepository.deleteAllByCarer(carer);
        for (PolicyType policyType : policyTypes) {
            interestPolicyTypeRepository.save(InterestPolicyType.builder()
                    .carer(carer)
                    .policyType(policyType)
                    .build());
        }

        return new InterestPolicyTypeUpdateResponse("관심 유형이 수정되었습니다.", typeIds);
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
