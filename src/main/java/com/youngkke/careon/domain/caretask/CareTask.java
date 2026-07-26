package com.youngkke.careon.domain.caretask;

import com.youngkke.careon.domain.carer.Cared;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 돌봄 대상자의 정기 안부·복약·할 일 "정의".
 *
 * <p>기존 todos 테이블(제도별 서류 체크)과는 별개다. todos는 저장한 제도에 필요한 서류를 보호자가 체크하는
 * 것이고, 이건 돌봄 대상자가 워치에서 수행하는 일이라 주체도 수명주기도 다르다.
 *
 * <p>완료 여부는 이 행에 두지 않는다. 매일 반복되는 항목은 "오늘 했는지"가 날짜마다 따로 필요하기 때문에
 * care_task_completion에 날짜별로 남긴다.
 */
@Entity
@Table(name = "care_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CareTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_task_id")
    private Integer careTaskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false)
    private Cared cared;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private CareTaskKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", nullable = false, length = 20)
    private CareTaskRepeat repeatType;

    /** 하루 중 언제 하는지. 매일 반복이든 하루짜리든 시각은 항상 필요하다. */
    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    /** ONCE일 때만 쓰는 날짜. 나머지 반복에서는 null. */
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    /** WEEKLY일 때만 쓰는 반복 요일. 나머지 반복에서는 비어 있다. */
    @Convert(converter = DaysOfWeekConverter.class)
    @Column(name = "days_of_week", length = 100)
    @Builder.Default
    private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

    /**
     * 보호자가 잠시 끄거나 지운 항목. 실제로 지우지 않는 이유는 이미 쌓인 완료 기록(care_task_completion)이
     * 함께 사라지면 "지난주에 약을 먹었는지"를 되돌아볼 수 없기 때문이다.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 이 할 일이 해당 날짜에 해야 하는 것인지. 꺼둔 항목은 어느 날에도 뜨지 않는다. */
    public boolean isScheduledOn(LocalDate date) {
        if (!active) {
            return false;
        }
        return switch (repeatType) {
            case DAILY -> true;
            case ONCE -> date.equals(scheduledDate);
            case WEEKLY -> daysOfWeek.contains(date.getDayOfWeek());
        };
    }

    /** 부분 수정: null인 값은 유지하고, 값이 있는 필드만 갱신한다. */
    public void update(
            String title,
            CareTaskKind kind,
            CareTaskRepeat repeatType,
            LocalTime scheduledTime,
            LocalDate scheduledDate,
            Set<DayOfWeek> daysOfWeek,
            Boolean active) {
        if (title != null) {
            this.title = title;
        }
        if (kind != null) {
            this.kind = kind;
        }
        if (repeatType != null) {
            this.repeatType = repeatType;
            // 반복 방식을 바꾸면 이전 방식에서만 쓰던 값이 남아 혼선을 주므로 함께 비운다.
            if (repeatType != CareTaskRepeat.ONCE) {
                this.scheduledDate = null;
            }
            if (repeatType != CareTaskRepeat.WEEKLY) {
                this.daysOfWeek = EnumSet.noneOf(DayOfWeek.class);
            }
        }
        if (scheduledTime != null) {
            this.scheduledTime = scheduledTime;
        }
        if (scheduledDate != null) {
            this.scheduledDate = scheduledDate;
        }
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            this.daysOfWeek = EnumSet.copyOf(daysOfWeek);
        }
        if (active != null) {
            this.active = active;
        }
    }

    public void deactivate() {
        this.active = false;
    }
}
