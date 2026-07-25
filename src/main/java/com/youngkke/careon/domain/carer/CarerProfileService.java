package com.youngkke.careon.domain.carer;

import com.youngkke.careon.domain.carer.dto.CaredCreateResponse;
import com.youngkke.careon.domain.carer.dto.CaredRequest;
import com.youngkke.careon.domain.carer.dto.CaredResponse;
import com.youngkke.careon.domain.carer.dto.DiagnosisProfileRequest;
import com.youngkke.careon.domain.carer.dto.DiagnosisProfileResponse;
import com.youngkke.careon.domain.carer.dto.IncomeSignalResponse;
import com.youngkke.careon.domain.carer.dto.IncomeSignalUpdateRequest;
import com.youngkke.careon.domain.carer.dto.IncomeSignalUpdateResponse;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 2단계 진단 프로필 / 돌봄 대상자 / 소득 추론 근거 관련 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarerProfileService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final CarerIncomeSignalRepository carerIncomeSignalRepository;

    /** 내 돌봄 대상자 목록 조회 (앱). 워치 페어링 등에서 cared_id를 선택할 때 사용한다. */
    public List<CaredResponse> getCaredList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return caredRepository.findAllByCarerOrderByCaredIdAsc(carer).stream()
                .map(this::toCaredResponse)
                .toList();
    }

    /** 내 진단 프로필 조회 (돌봄 대상자, 소득 추론 근거 포함). */
    public DiagnosisProfileResponse getDiagnosisProfile(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);

        List<CaredResponse> cared = caredRepository.findAllByCarerOrderByCaredIdAsc(carer).stream()
                .map(this::toCaredResponse)
                .toList();
        List<IncomeSignalResponse> signals = carerIncomeSignalRepository.findAllByCarerOrderBySignalIdAsc(carer).stream()
                .map(this::toIncomeSignalResponse)
                .toList();

        return new DiagnosisProfileResponse(
                carer.getCarerId(),
                carer.getAge(),
                carer.getHouseholdMembersCount(),
                carer.getCaredCount(),
                carer.getKnowHousingType(),
                carer.getHousingType(),
                carer.getBiggestBurdenType(),
                carer.getBurdenTypeReasonSummary(),
                carer.getDailyCareHoursSelf(),
                carer.getDailyCareHoursHousehold(),
                carer.getHasBackupCaregiver(),
                carer.getBackupCaregiverRelation(),
                carer.getMedicalBurdenLevel(),
                carer.getIsStudent(),
                carer.getHasIncomeActivity(),
                carer.getIncomeValueStatus(),
                carer.getIncomeAssessmentCriteria(),
                carer.getIncomeValue(),
                carer.getMedianIncomeRatio(),
                carer.getIncomeVariabilityType(),
                carer.getIncomeRelatedUtterance(),
                carer.getHasBasicLivelihoodSupport(),
                carer.getHasBasicLivelihoodSupportSource(),
                carer.getHasChaSangWi(),
                carer.getHasChaSangWiSource(),
                carer.getMilitaryServiceStatus(),
                carer.getMilitaryServiceExtensionYears(),
                carer.getHousingDeposit(),
                carer.getHousingMonthlyRent(),
                carer.getHouseholdAssetValue(),
                carer.getVehicleValue(),
                carer.getFinancialDetailStatus(),
                carer.isDiagnosisCompleted(),
                cared,
                signals);
    }

    /** 내 진단 프로필 수정. 보낸 필드만 수정한다. */
    @Transactional
    public MessageResponse updateDiagnosisProfile(Integer carerId, DiagnosisProfileRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        carer.updateDiagnosisProfile(request);
        return new MessageResponse("진단 프로필이 수정되었습니다.");
    }

    /** 돌봄 대상자 추가. */
    @Transactional
    public CaredCreateResponse addCared(Integer carerId, CaredRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository.save(Cared.builder()
                .carer(carer)
                .caredRelation(request.caredRelation())
                .age(request.age())
                .conditionSummary(request.conditionSummary())
                .severityLevel(request.severityLevel())
                .build());

        return new CaredCreateResponse(cared.getCaredId(), "돌봄 대상자가 추가되었습니다.");
    }

    /** 돌봄 대상자 수정. 본인 소유가 아니면 존재하지 않는 것으로 취급한다. */
    @Transactional
    public MessageResponse updateCared(Integer carerId, Integer caredId, CaredRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository.findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));

        cared.update(request.caredRelation(), request.age(), request.conditionSummary(), request.severityLevel());

        return new MessageResponse("돌봄 대상자가 수정되었습니다.");
    }

    /** 돌봄 대상자 삭제. */
    @Transactional
    public MessageResponse deleteCared(Integer carerId, Integer caredId) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository.findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));

        caredRepository.delete(cared);

        return new MessageResponse("돌봄 대상자가 삭제되었습니다.");
    }

    /** 소득 추론 근거 조회. */
    public List<IncomeSignalResponse> getIncomeSignals(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return carerIncomeSignalRepository.findAllByCarerOrderBySignalIdAsc(carer).stream()
                .map(this::toIncomeSignalResponse)
                .toList();
    }

    /** 소득 추론 충돌 해결. */
    @Transactional
    public IncomeSignalUpdateResponse updateIncomeSignal(
            Integer carerId, Integer signalId, IncomeSignalUpdateRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        CarerIncomeSignal signal = carerIncomeSignalRepository.findBySignalIdAndCarer(signalId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.INCOME_SIGNAL_NOT_FOUND));

        signal.resolve(request.contradictionResolved(), request.parsedValue());

        return new IncomeSignalUpdateResponse(
                signal.getSignalId(), signal.getContradictionResolved(), "소득 추론 근거가 수정되었습니다.");
    }

    private CaredResponse toCaredResponse(Cared cared) {
        return new CaredResponse(
                cared.getCaredId(),
                cared.getCaredRelation(),
                cared.getAge(),
                cared.getConditionSummary(),
                cared.getSeverityLevel());
    }

    private IncomeSignalResponse toIncomeSignalResponse(CarerIncomeSignal signal) {
        return new IncomeSignalResponse(
                signal.getSignalId(),
                signal.getSignalType(),
                signal.getRawValue(),
                signal.getParsedValue(),
                signal.getSource(),
                signal.getConfidence(),
                signal.getContradictsSignalId(),
                signal.getContradictionResolved(),
                DateTimes.toIsoString(signal.getCreatedAt()));
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
