package com.youngkke.careon.domain.carer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 돌봄 대상자(cared) 테이블. 유저(carer) 한 명이 여러 돌봄 대상자를 가질 수 있다. */
@Entity
@Table(name = "cared")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Cared {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cared_id")
    private Integer caredId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    /** 관계 (예: 어머니, 아버지) */
    @Column(name = "cared_relation", length = 30)
    private String caredRelation;

    @Column(name = "age")
    private Integer age;

    @Column(name = "condition_summary", columnDefinition = "TEXT")
    private String conditionSummary;

    /** 중증도 (예: 중증, 경증) */
    @Column(name = "severity_level", length = 20)
    private String severityLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 부분 수정: null인 값은 유지하고, 값이 있는 필드만 갱신한다. */
    public void update(String caredRelation, Integer age, String conditionSummary, String severityLevel) {
        if (caredRelation != null) {
            this.caredRelation = caredRelation;
        }
        if (age != null) {
            this.age = age;
        }
        if (conditionSummary != null) {
            this.conditionSummary = conditionSummary;
        }
        if (severityLevel != null) {
            this.severityLevel = severityLevel;
        }
    }
}
