package com.youngkke.careon.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * accessToken / refreshToken 발급 및 검증을 담당한다.
 * 로그인 API 명세에 따라 웹 accessToken은 6시간, 앱 accessToken은 1시간, refreshToken(앱 전용)은 90일 유효.
 */
@Getter
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String TYPE_WEAR_ACCESS = "wear_access";
    private static final String TYPE_WEAR_REFRESH = "wear_refresh";

    private final SecretKey key;
    private final long webAccessTokenValiditySeconds;
    private final long appAccessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;
    private final long wearAccessTokenValiditySeconds;
    private final long wearRefreshTokenValiditySeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.web-access-token-validity-seconds}") long webAccessTokenValiditySeconds,
            @Value("${jwt.app-access-token-validity-seconds}") long appAccessTokenValiditySeconds,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds,
            @Value("${wear.access-token-validity-seconds}") long wearAccessTokenValiditySeconds,
            @Value("${wear.refresh-token-validity-seconds}") long wearRefreshTokenValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.webAccessTokenValiditySeconds = webAccessTokenValiditySeconds;
        this.appAccessTokenValiditySeconds = appAccessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
        this.wearAccessTokenValiditySeconds = wearAccessTokenValiditySeconds;
        this.wearRefreshTokenValiditySeconds = wearRefreshTokenValiditySeconds;
    }

    public String createAccessToken(Integer userId, long validitySeconds) {
        return createToken(userId, TYPE_ACCESS, validitySeconds);
    }

    public String createRefreshToken(Integer userId) {
        return createToken(userId, TYPE_REFRESH, refreshTokenValiditySeconds);
    }

    public Instant getRefreshTokenExpiry() {
        return Instant.now().plusSeconds(refreshTokenValiditySeconds);
    }

    /** 토큰의 subject(userId)를 반환한다. 서명/만료가 유효하지 않으면 io.jsonwebtoken.JwtException이 발생한다. */
    public Integer getCarerId(String token) {
        return getSubjectId(token);
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public String createWearAccessToken(Integer wearDeviceId) {
        return createToken(wearDeviceId, TYPE_WEAR_ACCESS, wearAccessTokenValiditySeconds);
    }

    public String createWearRefreshToken(Integer wearDeviceId) {
        return createToken(wearDeviceId, TYPE_WEAR_REFRESH, wearRefreshTokenValiditySeconds);
    }

    /** 토큰의 subject(wearDeviceId)를 반환한다. 서명/만료가 유효하지 않으면 io.jsonwebtoken.JwtException이 발생한다. */
    public Integer getWearDeviceId(String token) {
        return getSubjectId(token);
    }

    public boolean isWearAccessToken(String token) {
        return TYPE_WEAR_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isWearRefreshToken(String token) {
        return TYPE_WEAR_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    private Integer getSubjectId(String token) {
        return Integer.valueOf(parseClaims(token).getSubject());
    }

    private String createToken(Integer userId, String type, long validitySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
