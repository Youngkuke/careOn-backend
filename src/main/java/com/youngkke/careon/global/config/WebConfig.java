package com.youngkke.careon.global.config;

import com.youngkke.careon.global.auth.CurrentCarerIdArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentCarerIdArgumentResolver currentCarerIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentCarerIdArgumentResolver);
    }

    /**
     * 프론트엔드(웹)에서 브라우저로 API를 직접 호출할 수 있게 허용하는 출처 목록.
     * 로컬 개발 주소 + 배포된 Vercel 주소(고정 도메인 + 프리뷰 배포 와일드카드)를 허용한다.
     * 나중에 실제 서비스 도메인이 정해지면 여기 추가하면 된다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://care-on-gamma.vercel.app",
                        "https://care-*-pjsowo0-4448s-projects.vercel.app")
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
