package com.youngkke.careon.domain.carer;

import com.youngkke.careon.domain.caretask.CareTask;
import com.youngkke.careon.domain.caretask.CareTaskCompletionRepository;
import com.youngkke.careon.domain.caretask.CareTaskRepository;
import com.youngkke.careon.domain.notification.NotificationRepository;
import com.youngkke.careon.domain.timeline.CareEventRepository;
import com.youngkke.careon.domain.wear.EmergencyEventRepository;
import com.youngkke.careon.domain.wear.HeartRateRepository;
import com.youngkke.careon.domain.wear.SafeZoneEventRepository;
import com.youngkke.careon.domain.wear.SafeZoneRepository;
import com.youngkke.careon.domain.wear.WearDeviceRepository;
import com.youngkke.careon.domain.wear.WearPairingCodeRepository;
import com.youngkke.careon.domain.policy.InterestPolicyType;
import com.youngkke.careon.domain.policy.InterestPolicyTypeRepository;
import com.youngkke.careon.domain.policy.PolicyType;
import com.youngkke.careon.domain.policy.PolicyTypeRepository;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.policy.SavedPolicyRepository;
import com.youngkke.careon.domain.todo.TodoRepository;
import com.youngkke.careon.domain.carer.dto.AppInstallStatusRequest;
import com.youngkke.careon.domain.carer.dto.AppInstallStatusResponse;
import com.youngkke.careon.domain.document.UserDocumentHistoryRepository;
import com.youngkke.careon.domain.policy.MatchedPolicyRepository;
import com.youngkke.careon.domain.push.PushTokenRepository;
import com.youngkke.careon.global.util.DateTimes;
import com.youngkke.careon.domain.carer.dto.AppLoginResponse;
import com.youngkke.careon.domain.carer.dto.AppMeResponse;
import com.youngkke.careon.domain.carer.dto.LoginRequest;
import com.youngkke.careon.domain.carer.dto.PasswordResetLinkRequest;
import com.youngkke.careon.domain.carer.dto.PasswordResetRequest;
import com.youngkke.careon.domain.carer.dto.RefreshTokenRequest;
import com.youngkke.careon.domain.carer.dto.RefreshTokenResponse;
import com.youngkke.careon.domain.carer.dto.SignupRequest;
import com.youngkke.careon.domain.carer.dto.SignupResponse;
import com.youngkke.careon.domain.carer.dto.UpdateAppProfileRequest;
import com.youngkke.careon.domain.carer.dto.UpdateWebProfileRequest;
import com.youngkke.careon.domain.carer.dto.WebLoginResponse;
import com.youngkke.careon.domain.carer.dto.WebMeResponse;
import com.youngkke.careon.global.auth.JwtProvider;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.mail.MailService;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarerService {

    private final CarerRepository carerRepository;
    private final PolicyTypeRepository policyTypeRepository;
    private final InterestPolicyTypeRepository interestPolicyTypeRepository;
    private final SavedPolicyRepository savedPolicyRepository;
    private final TodoRepository todoRepository;
    private final NotificationRepository notificationRepository;
    private final CaredRepository caredRepository;
    private final CarerIncomeSignalRepository carerIncomeSignalRepository;
    private final MatchedPolicyRepository matchedPolicyRepository;
    private final UserDocumentHistoryRepository userDocumentHistoryRepository;
    private final PushTokenRepository pushTokenRepository;
    private final CareTaskRepository careTaskRepository;
    private final CareTaskCompletionRepository careTaskCompletionRepository;
    private final CareEventRepository careEventRepository;
    private final HeartRateRepository heartRateRepository;
    private final SafeZoneRepository safeZoneRepository;
    private final SafeZoneEventRepository safeZoneEventRepository;
    private final EmergencyEventRepository emergencyEventRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final WearPairingCodeRepository wearPairingCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final MailService mailService;

    private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (carerRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        List<PolicyType> policyTypes = request.interestPolicyTypeIdsOrEmpty().stream()
                .map(id -> policyTypeRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_TYPE_NOT_FOUND)))
                .toList();

        Carer carer = Carer.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .region(request.region())
                .termsAgreed(true)
                .installPromptCount(0)
                .appInstalled(false)
                .notificationEnabled(true)
                .diagnosisCompleted(false)
                .build();
        carerRepository.save(carer);

        for (PolicyType policyType : policyTypes) {
            interestPolicyTypeRepository.save(InterestPolicyType.builder()
                    .carer(carer)
                    .policyType(policyType)
                    .build());
        }

        String accessToken =
                jwtProvider.createAccessToken(carer.getCarerId(), jwtProvider.getWebAccessTokenValiditySeconds());

        return new SignupResponse(carer.getCarerId(), accessToken, carer.isDiagnosisCompleted());
    }

    public WebLoginResponse loginWeb(LoginRequest request) {
        Carer carer = authenticate(request);
        String accessToken =
                jwtProvider.createAccessToken(carer.getCarerId(), jwtProvider.getWebAccessTokenValiditySeconds());
        return new WebLoginResponse(carer.getCarerId(), accessToken, carer.isDiagnosisCompleted());
    }

    @Transactional
    public AppLoginResponse loginApp(LoginRequest request) {
        Carer carer = authenticate(request);
        String accessToken =
                jwtProvider.createAccessToken(carer.getCarerId(), jwtProvider.getAppAccessTokenValiditySeconds());
        String refreshToken = jwtProvider.createRefreshToken(carer.getCarerId());
        LocalDateTime expiresAt = toKstLocalDateTime(jwtProvider.getRefreshTokenExpiry());
        carer.updateRefreshToken(refreshToken, expiresAt);
        return new AppLoginResponse(
                carer.getCarerId(), accessToken, refreshToken, DateTimes.toIsoString(expiresAt));
    }

    @Transactional
    public MessageResponse logout(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        carer.clearRefreshToken();
        return new MessageResponse("로그아웃되었습니다.");
    }

    @Transactional
    public RefreshTokenResponse reissueToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Integer userId;
        try {
            if (!jwtProvider.isRefreshToken(refreshToken)) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            userId = jwtProvider.getCarerId(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Carer carer = carerRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        // 기존에 발급했던 refreshToken과 다르면(이미 재발급되어 교체됐거나 위조된 경우) 거부한다.
        if (!refreshToken.equals(carer.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken =
                jwtProvider.createAccessToken(userId, jwtProvider.getAppAccessTokenValiditySeconds());
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        LocalDateTime expiresAt = toKstLocalDateTime(jwtProvider.getRefreshTokenExpiry());
        carer.updateRefreshToken(newRefreshToken, expiresAt);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken, DateTimes.toIsoString(expiresAt));
    }

    public WebMeResponse getWebMe(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        return new WebMeResponse(
                carer.getCarerId(),
                carer.getName(),
                carer.getEmail(),
                carer.getRegion(),
                carer.isDiagnosisCompleted(),
                carer.isAppInstalled(),
                carer.getInstallPromptCount(),
                carer.isNotificationEnabled());
    }

    public AppMeResponse getAppMe(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        return new AppMeResponse(
                carer.getCarerId(), carer.getName(), carer.getEmail(), carer.getRegion(), carer.isNotificationEnabled());
    }

    @Transactional
    public MessageResponse updateWebProfile(Integer userId, UpdateWebProfileRequest request) {
        Carer carer = getCarerOrThrow(userId);
        checkEmailDuplicate(carer, request.email());
        String encodedPassword = encodeIfPresent(request.password());
        carer.updateProfile(
                request.name(), request.email(), encodedPassword, request.region(), request.notificationEnabled());
        return new MessageResponse("회원 정보가 수정되었습니다.");
    }

    @Transactional
    public MessageResponse updateAppProfile(Integer userId, UpdateAppProfileRequest request) {
        Carer carer = getCarerOrThrow(userId);
        checkEmailDuplicate(carer, request.email());
        String encodedPassword = encodeIfPresent(request.password());
        carer.updateProfile(
                request.name(), request.email(), encodedPassword, request.region(), request.notificationEnabled());
        return new MessageResponse("회원 정보가 수정되었습니다.");
    }

    @Transactional
    public AppInstallStatusResponse updateAppInstallStatus(Integer userId, AppInstallStatusRequest request) {
        Carer carer = getCarerOrThrow(userId);
        if (Boolean.TRUE.equals(request.appInstalled())) {
            carer.markAppInstalled();
        } else {
            carer.increaseInstallPromptCount();
        }
        return new AppInstallStatusResponse(
                "처리되었습니다.", carer.isAppInstalled(), carer.getInstallPromptCount());
    }

    /**
     * 회원 탈퇴(웹/앱 공통). 유저를 참조하는 데이터를 전부 정리한 뒤 유저를 삭제한다.
     *
     * <p>FK 삭제 규칙이 모두 NO ACTION이라 참조가 하나라도 남아 있으면 DB가 삭제를 막는다. 그러면
     * 탈퇴가 500으로 실패하고 사용자는 이유를 알 수 없으므로, 자식부터 부모 순으로 빠짐없이 지운다.
     *
     * <p>지우는 순서가 곧 참조 관계다. 새 테이블이 유저나 피보호자를 참조하게 되면 여기에도 추가해야 한다.
     */
    @Transactional
    public MessageResponse withdraw(Integer userId) {
        Carer carer = getCarerOrThrow(userId);

        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAllByCarer(carer);
        if (!savedPolicies.isEmpty()) {
            notificationRepository.deleteAllBySavedPolicyIn(savedPolicies);
            todoRepository.deleteAllBySavedPolicyIn(savedPolicies);
            savedPolicyRepository.deleteAllByCarer(carer);
        }
        interestPolicyTypeRepository.deleteAllByCarer(carer);

        // 챗봇/진단 관련 데이터도 함께 정리한다 (FK 제약 위반 방지).
        pushTokenRepository.deleteAllByCarer(carer);
        userDocumentHistoryRepository.deleteAll(userDocumentHistoryRepository.findAllWithDetailByCarer(carer));
        matchedPolicyRepository.deleteAll(matchedPolicyRepository.findAllWithPolicyByCarer(carer));
        carerIncomeSignalRepository.deleteAll(carerIncomeSignalRepository.findAllByCarerOrderBySignalIdAsc(carer));

        deleteCaredData(carer);

        carerRepository.delete(carer);

        return new MessageResponse("회원 탈퇴가 완료되었습니다.");
    }

    /**
     * 피보호자와 거기에 딸린 워치·돌봄 데이터를 정리한다.
     *
     * <p>safe_zone/wear_pairing_code의 created_by_carer_id와 emergency_event의 acknowledged_by도
     * 보호자를 참조하지만, 보호자는 자기 피보호자만 등록·관리할 수 있어 피보호자 기준으로 지우면 전부 걸린다.
     */
    private void deleteCaredData(Carer carer) {
        List<Cared> caredList = caredRepository.findAllByCarerOrderByCaredIdAsc(carer);
        if (caredList.isEmpty()) {
            return;
        }

        List<CareTask> careTasks = careTaskRepository.findAllByCaredIn(caredList);
        if (!careTasks.isEmpty()) {
            careTaskCompletionRepository.deleteAllByCareTaskIn(careTasks);
        }

        // 워치(wear_device)를 참조하는 것들을 먼저 지운 뒤에 워치를 지운다.
        safeZoneEventRepository.deleteAllByCaredIn(caredList);
        emergencyEventRepository.deleteAllByCaredIn(caredList);
        heartRateRepository.deleteAllByCaredIn(caredList);
        careEventRepository.deleteAllByCaredIn(caredList);
        careTaskRepository.deleteAllByCaredIn(caredList);
        safeZoneRepository.deleteAllByCaredIn(caredList);
        wearDeviceRepository.deleteAllByCaredIn(caredList);
        wearPairingCodeRepository.deleteAllByCaredIn(caredList);

        caredRepository.deleteAll(caredList);
    }

    /**
     * 비밀번호 재설정 링크 발송. 계정 존재 여부를 노출하지 않기 위해, 가입 여부와 무관하게 항상 같은 응답을 준다.
     * 실제 이메일 발송/토큰 발급은 계정이 존재할 때만 내부적으로 수행한다.
     */
    @Transactional
    public MessageResponse sendPasswordResetLink(PasswordResetLinkRequest request) {
        carerRepository.findByEmail(request.email()).ifPresent(carer -> {
            String resetToken = UUID.randomUUID().toString().replace("-", "");
            carer.issueResetToken(resetToken, LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
            mailService.sendPasswordResetEmail(carer.getEmail(), resetToken);
        });
        return new MessageResponse("비밀번호 재설정을 위해 이메일을 확인해보세요.");
    }

    @Transactional
    public MessageResponse resetPassword(PasswordResetRequest request) {
        Carer carer = carerRepository.findByResetToken(request.resetToken())
                .filter(u -> u.getResetTokenExpiresAt() != null
                        && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul"))))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_LINK_INVALID));

        carer.updateProfile(null, null, passwordEncoder.encode(request.newPassword()), null, null);
        carer.clearResetToken();

        return new MessageResponse("비밀번호가 재설정되었습니다.");
    }

    private void checkEmailDuplicate(Carer carer, String newEmail) {
        if (newEmail != null && !newEmail.equalsIgnoreCase(carer.getEmail()) && carerRepository.existsByEmail(newEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private String encodeIfPresent(String rawPassword) {
        return rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
    }

    private Carer getCarerOrThrow(Integer userId) {
        return carerRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private Carer authenticate(LoginRequest request) {
        Carer carer = carerRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), carer.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return carer;
    }

    private LocalDateTime toKstLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
    }
}
