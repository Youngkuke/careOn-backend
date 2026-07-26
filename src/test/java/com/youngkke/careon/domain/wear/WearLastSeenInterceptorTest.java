package com.youngkke.careon.domain.wear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.global.auth.JwtProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 미통신 알림은 last_seen_at 하나로 판단하므로, 워치가 살아 있는데 미통신으로 잡히면 오알림이 나간다.
 * 읽기만 하는 요청도 "워치가 살아 있다"는 신호라는 점을 회귀 테스트로 고정해둔다.
 */
@SpringBootTest
@Transactional
class WearLastSeenInterceptorTest {

    /** 실제 푸시를 보내지 않도록 막는다. 이 테스트가 확인하려는 건 발송이 아니라 시각 갱신이다. */
    @MockitoBean
    private com.youngkke.careon.domain.push.ExpoPushClient expoPushClient;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CarerRepository carerRepository;

    @Autowired
    private CaredRepository caredRepository;

    @Autowired
    private WearDeviceRepository wearDeviceRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void 읽기만_하는_워치_요청도_last_seen_at을_갱신한다() throws Exception {
        LocalDateTime longAgo = LocalDateTime.now().minusDays(2);
        WearDevice wearDevice = givenPairedWearDevice(longAgo);
        String token = jwtProvider.createWearAccessToken(wearDevice.getWearDeviceId());

        // 안심 구역을 설정한 적이 없어 204가 나오지만, 요청이 성공한 것 자체가 살아 있다는 신호다.
        mockMvc.perform(get("/api/wear/safe-zone").header("Authorization", "Bearer " + token))
                .andExpect(result -> assertEquals(204, result.getResponse().getStatus()));

        assertTrue(
                wearDevice.getLastSeenAt().isAfter(longAgo),
                "읽기 요청 뒤에도 last_seen_at이 그대로다: " + wearDevice.getLastSeenAt());
    }

    @Test
    void 인증에_실패한_요청은_last_seen_at을_갱신하지_않는다() throws Exception {
        LocalDateTime longAgo = LocalDateTime.now().minusDays(2);
        WearDevice wearDevice = givenPairedWearDevice(longAgo);

        mockMvc.perform(get("/api/wear/safe-zone").header("Authorization", "Bearer 잘못된토큰"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() >= 400));

        assertEquals(longAgo, wearDevice.getLastSeenAt(), "인증 실패인데 last_seen_at이 갱신됐다");
    }

    @Test
    void 해제된_워치의_요청은_last_seen_at을_갱신하지_않는다() throws Exception {
        LocalDateTime longAgo = LocalDateTime.now().minusDays(2);
        WearDevice wearDevice = givenPairedWearDevice(longAgo);
        String token = jwtProvider.createWearAccessToken(wearDevice.getWearDeviceId());
        wearDevice.disconnect(LocalDateTime.now()); // 해제는 lastSeenAt을 건드리지 않는다

        mockMvc.perform(get("/api/wear/safe-zone").header("Authorization", "Bearer " + token))
                .andExpect(result -> assertEquals(401, result.getResponse().getStatus()));

        assertEquals(longAgo, wearDevice.getLastSeenAt(), "해제된 워치인데 last_seen_at이 갱신됐다");
    }

    private WearDevice givenPairedWearDevice(LocalDateTime lastSeenAt) {
        Carer carer = carerRepository.save(Carer.builder()
                .name("테스트보호자")
                .email("last-seen-" + System.nanoTime() + "@test.com")
                .password("encoded")
                .build());
        Cared cared = caredRepository.save(Cared.builder().carer(carer).build());
        return wearDeviceRepository.save(WearDevice.builder()
                .cared(cared)
                .deviceName("테스트 워치")
                .refreshTokenHash("hash")
                .connectedAt(lastSeenAt)
                .lastSeenAt(lastSeenAt)
                .build());
    }
}
