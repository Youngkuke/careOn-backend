package com.youngkke.careon.global.config;

import com.youngkke.careon.domain.wear.WearLastSeenInterceptor;
import com.youngkke.careon.global.auth.CurrentCarerIdArgumentResolver;
import com.youngkke.careon.global.auth.CurrentWearDeviceIdArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentCarerIdArgumentResolver currentCarerIdArgumentResolver;
    private final CurrentWearDeviceIdArgumentResolver currentWearDeviceIdArgumentResolver;
    private final WearLastSeenInterceptor wearLastSeenInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentCarerIdArgumentResolver);
        resolvers.add(currentWearDeviceIdArgumentResolver);
    }

    /**
     * 워치 요청은 종류를 가리지 않고 last_seen_at을 갱신한다.
     * pair/refresh는 아직 워치 access token으로 오는 요청이 아니라 제외하고, 해당 서비스에서 직접 처리한다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(wearLastSeenInterceptor)
                .addPathPatterns("/api/wear/**")
                .excludePathPatterns("/api/wear/auth/**");
    }

    // CORS 설정은 CorsConfig 한 곳으로 통합했다. (configurer가 두 개면 매핑이 충돌할 수 있음)
}
