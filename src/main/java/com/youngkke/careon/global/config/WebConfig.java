package com.youngkke.careon.global.config;

import com.youngkke.careon.global.auth.CurrentCarerIdArgumentResolver;
import com.youngkke.careon.global.auth.CurrentWearDeviceIdArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentCarerIdArgumentResolver currentCarerIdArgumentResolver;
    private final CurrentWearDeviceIdArgumentResolver currentWearDeviceIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentCarerIdArgumentResolver);
        resolvers.add(currentWearDeviceIdArgumentResolver);
    }

    // CORS 설정은 CorsConfig 한 곳으로 통합했다. (configurer가 두 개면 매핑이 충돌할 수 있음)
}
