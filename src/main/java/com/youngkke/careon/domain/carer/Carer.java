package com.youngkke.careon.domain.carer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * ERD의 "가족돌봄청년(carers)" 테이블. (구 users 테이블에서 개명됨)
 * 챗봇 진단용 신규 컬럼들은 아직 로직에서 사용하지 않으며, DB 스키마와 맞추기 위해 필드로만 선언해둔다.
 */
@Entity
@Table(name = "carers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Carer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "carer_id")
    private Integer carerId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "region", length = 20)
    private String region;

    @Column(name = "terms_agreed", nullable = false)
    private boolean termsAgreed;

    @Column(name = "install_prompt_count", nullable = false)
    private int installPromptCount;

    @Column(name = "app_installed", nullable = false)
    private boolean appInstalled;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "diagnosis_completed", nullable = false)
    private boolean diagnosisCompleted;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    // ===== 이하 챗봇 진단용 신규 컬럼 (아직 로직 미사용, 스키마 매핑용) =====

    @Column(name = "age")
    private Integer age;

    @Column(name = "household_members_count")
    private Integer householdMembersCount;

    @Column(name = "cared_count")
    private Integer caredCount;

    @Column(name = "know_housing_type", length = 30)
    private String knowHousingType;

    @Column(name = "housing_type", length = 50)
    private String housingType;

    @Column(name = "biggest_burden_type", length = 100)
    private String biggestBurdenType;

    @Column(name = "burden_type_reason_summary", length = 500)
    private String burdenTypeReasonSummary;

    @Column(name = "daily_care_hours_self", length = 30)
    private String dailyCareHoursSelf;

    @Column(name = "daily_care_hours_household", length = 30)
    private String dailyCareHoursHousehold;

    @Column(name = "has_backup_caregiver")
    private Boolean hasBackupCaregiver;

    @Column(name = "backup_caregiver_relation", length = 50)
    private String backupCaregiverRelation;

    @Column(name = "medical_burden_level", length = 10)
    private String medicalBurdenLevel;

    @Column(name = "is_student")
    private Boolean isStudent;

    @Column(name = "has_income_activity")
    private Boolean hasIncomeActivity;

    @Column(name = "income_value_status", length = 50)
    private String incomeValueStatus;

    @Column(name = "income_assessment_criteria", length = 50)
    private String incomeAssessmentCriteria;

    @Column(name = "income_value")
    private Integer incomeValue;

    @Column(name = "median_income_ratio", length = 30)
    private String medianIncomeRatio;

    @Column(name = "income_variability_type", length = 50)
    private String incomeVariabilityType;

    @Column(name = "income_related_utterance", columnDefinition = "TEXT")
    private String incomeRelatedUtterance;

    @Column(name = "has_basic_livelihood_support")
    private Boolean hasBasicLivelihoodSupport;

    @Column(name = "has_basic_livelihood_support_source")
    private Boolean hasBasicLivelihoodSupportSource;

    @Column(name = "has_cha_sang_wi")
    private Boolean hasChaSangWi;

    @Column(name = "has_cha_sang_wi_source")
    private Boolean hasChaSangWiSource;

    @Column(name = "military_service_status", length = 30)
    private String militaryServiceStatus;

    @Column(name = "military_service_extension_years")
    private Integer militaryServiceExtensionYears;

    @Column(name = "housing_deposit")
    private Integer housingDeposit;

    @Column(name = "housing_monthly_rent")
    private Integer housingMonthlyRent;

    @Column(name = "household_asset_value")
    private Integer householdAssetValue;

    @Column(name = "vehicle_value")
    private Integer vehicleValue;

    @Column(name = "financial_detail_status", length = 30)
    private String financialDetailStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 앱 로그인/토큰 재발급 시 refreshToken을 갱신한다. */
    public void updateRefreshToken(String refreshToken, LocalDateTime refreshTokenExpiresAt) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    /** 로그아웃/탈퇴 시 refreshToken을 무효화한다. */
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiresAt = null;
    }

    /** 회원정보 수정: null인 값은 그대로 유지하고, 값이 있는 필드만 갱신한다. */
    public void updateProfile(String name, String email, String encodedPassword, String region, Boolean notificationEnabled) {
        if (name != null) {
            this.name = name;
        }
        if (email != null) {
            this.email = email;
        }
        if (encodedPassword != null) {
            this.password = encodedPassword;
        }
        if (region != null) {
            this.region = region;
        }
        if (notificationEnabled != null) {
            this.notificationEnabled = notificationEnabled;
        }
    }

    /** 앱 설치 유도 모달에서 [설치했어요]를 누른 경우. */
    public void markAppInstalled() {
        this.appInstalled = true;
    }

    /** 앱 설치 유도 모달에서 [나중에 할게요]를 누른 경우. 노출 횟수를 1 늘린다. */
    public void increaseInstallPromptCount() {
        this.installPromptCount += 1;
    }

    /** 비밀번호 재설정 링크 발송 시 토큰을 발급한다. */
    public void issueResetToken(String resetToken, LocalDateTime expiresAt) {
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = expiresAt;
    }

    /** 비밀번호 재설정 완료 후 토큰을 1회용으로 소진 처리한다. */
    public void clearResetToken() {
        this.resetToken = null;
        this.resetTokenExpiresAt = null;
    }
}
