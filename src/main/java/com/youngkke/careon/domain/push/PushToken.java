package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.carer.Carer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 앱이 발급받아 등록한 Expo 푸시 토큰. 보호자 1명이 기기 여러 대를 쓸 수 있어 carer당 여러 건을 허용한다.
 * 토큰 자체에는 유니크 제약을 둔다. 한 기기를 여러 계정이 번갈아 로그인하면 Expo는 같은 토큰을 그대로 주는데,
 * 이때 row를 새로 만들면 이전 사용자에게 갈 푸시가 지금 로그인한 사람 기기로 새어나가기 때문이다.
 * (같은 토큰이 다시 등록되면 소유자를 지금 로그인한 보호자로 옮긴다.)
 */
@Entity
@Table(name = "push_tokens", uniqueConstraints = @UniqueConstraint(columnNames = {"token"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_token_id")
    private Integer pushTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    /** 예: ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx] */
    @Column(name = "token", nullable = false, length = 255)
    private String token;

    /** 현재는 "expo"만 들어온다. 나중에 FCM/APNs를 직접 붙일 때를 대비해 값으로 남겨둔다. */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 이미 등록된 토큰이 다시 등록될 때, 소유자를 지금 로그인한 보호자로 옮긴다. */
    public void reassign(Carer carer, String platform) {
        this.carer = carer;
        this.platform = platform;
    }
}
