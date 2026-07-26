package com.youngkke.careon.domain.carer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 회원가입에서 관심 제도 유형은 선택 항목이다.
 *
 * <p>이 값은 맞춤 제도 목록의 그룹 정렬 기준으로만 쓰이고, 가입 후 별도 API로 언제든 설정할 수 있다.
 * 웹 가입 화면에서 입력을 받지 않기로 해 필수를 풀었고, 다시 필수로 되돌아가면 가입이 막히므로 고정해둔다.
 */
@SpringBootTest
@Transactional
class SignupInterestTypeOptionalTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void 관심_유형_없이도_가입된다() throws Exception {
        String body =
                """
                {
                  "name": "테스트",
                  "email": "no-interest-%d@test.com",
                  "password": "test1234",
                  "region": "관악구",
                  "terms_agreed": true
                }
                """
                        .formatted(System.nanoTime());

        mockMvc.perform(post("/api/web/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertEquals(
                        201, result.getResponse().getStatus(), "응답 본문: " + result.getResponse().getContentAsString()));
    }

    @Test
    void 관심_유형을_빈_배열로_보내도_가입된다() throws Exception {
        String body =
                """
                {
                  "name": "테스트",
                  "email": "empty-interest-%d@test.com",
                  "password": "test1234",
                  "region": "관악구",
                  "terms_agreed": true,
                  "interest_policy_type_ids": []
                }
                """
                        .formatted(System.nanoTime());

        mockMvc.perform(post("/api/web/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertEquals(
                        201, result.getResponse().getStatus(), "응답 본문: " + result.getResponse().getContentAsString()));
    }
}
