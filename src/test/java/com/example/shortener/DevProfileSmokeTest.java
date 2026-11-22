package com.example.shortener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The local "dev" profile (H2, no Redis, no Flyway) must start and serve the core flows. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevProfileSmokeTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void coreFlowsWorkOnH2() throws Exception {
        String body = mvc.perform(post("/api/v1/urls/shorten").with(jwt().jwt(j -> j.subject("dev-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://example.com/dev\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = json.readTree(body).path("data").path("shortCode").asText();

        mvc.perform(get("/" + code).header("User-Agent", "Mozilla/5.0 Chrome/130")).andExpect(status().isFound());
        mvc.perform(get("/api/v1/urls?q=dev").with(jwt().jwt(j -> j.subject("dev-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1));
        mvc.perform(get("/api/v1/urls/" + code + "/analytics").with(jwt().jwt(j -> j.subject("dev-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clicksTodayByHour.length()").value(24));
        mvc.perform(get("/api/v1/urls/summary").with(jwt().jwt(j -> j.subject("dev-user"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }
}
