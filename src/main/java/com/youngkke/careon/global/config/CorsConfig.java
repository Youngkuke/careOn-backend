package com.youngkke.careon.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 프로젝트의 단일 CORS 설정 지점.
     * 로컬 개발 주소 + 배포 주소(고정 도메인 + Vercel 프리뷰 와일드카드)를 허용한다.
     * allowCredentials(true) + 와일드카드 패턴을 함께 쓰기 위해 allowedOriginPatterns 사용.
     *
     * <p>쓰지 않게 된 주소는 목록에서 바로 뺀다. Vercel 배포를 내리면 그 서브도메인은 다른 사람이 다시
     * 등록할 수 있는데, 허용 목록에 남아 있으면 그 사람이 우리 API에 대해 인증 포함 요청을 보낼 수 있는
     * 출처가 된다. (2026-07-30 care-on-gamma.vercel.app 제거 — 프런트 확인 결과 배포가 내려감)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "https://careon-front-web.vercel.app",
                        "https://careon.site",
                        "https://www.careon.site",
                        "https://care-*-pjsowo0-4448s-projects.vercel.app",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}