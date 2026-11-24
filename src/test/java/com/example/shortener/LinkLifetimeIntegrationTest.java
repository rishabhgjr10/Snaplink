package com.example.shortener;

import com.example.shortener.repository.UrlRepository;
import com.example.shortener.service.ExpiredLinkCleanupJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Expiry, start time, click limits, anonymous lifetime cap and cleanup, driven by a movable clock. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinkLifetimeIntegrationTest extends MySqlContainerSupport {

    // Far from "now" so validation against the real date never interferes.
    static final MutableClock CLOCK = new MutableClock(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    private static final String BROWSER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0 Safari/537.36";
    private static final AtomicInteger VISITOR = new AtomicInteger();

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return CLOCK;
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UrlRepository urlRepository;
    @Autowired ExpiredLinkCleanupJob cleanupJob;

    private static RequestPostProcessor user(String uid) {
        return jwt().jwt(j -> j.subject(uid));
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private JsonNode create(String uid, Map<String, Object> body) throws Exception {
        var req = post("/api/v1/urls/shorten").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        if (uid != null) {
            req = req.with(user(uid));
        }
        String res = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).path("data");
    }

    private int redirectStatus(String code) throws Exception {
        String ip = "203.0.113." + VISITOR.incrementAndGet() % 250;
        return mvc.perform(get("/" + code).header("User-Agent", BROWSER).with(r -> { r.setRemoteAddr(ip); return r; }))
                .andReturn().getResponse().getStatus();
    }

    private void awaitClicks(String code, long expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (urlRepository.findByShortCode(code).orElseThrow().getClickCount() < expected) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("click count did not reach " + expected);
            }
            Thread.sleep(50);
        }
    }

    @Test
    void expiredLinksStopRedirectingEvenWhenCached() throws Exception {
        String uid = unique("exp-");
        Instant expiresAt = CLOCK.instant().plus(Duration.ofHours(1));
        JsonNode link = create(uid, Map.of("originalUrl", "https://example.com/soon", "expiresAt", expiresAt.toString()));
        String code = link.path("shortCode").asText();
        assertThat(Instant.parse(link.path("expiresAt").asText())).isEqualTo(expiresAt);

        assertThat(redirectStatus(code)).isEqualTo(302); // now also in the cache
        CLOCK.advance(Duration.ofMinutes(61));
        mvc.perform(get("/" + code))
                .andExpect(status().isGone())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("This link has expired")));

        mvc.perform(get("/api/v1/urls?status=expired").with(user(uid)))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].status").value("EXPIRED"));
        mvc.perform(get("/api/v1/urls?status=active").with(user(uid)))
                .andExpect(jsonPath("$.data.totalItems").value(0));

        // Reactivate by setting a new expiry.
        mvc.perform(put("/api/v1/urls/" + code).with(user(uid)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("expiresAt", CLOCK.instant().plus(Duration.ofDays(2)).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(redirectStatus(code)).isEqualTo(302);

        // Make it permanent.
        mvc.perform(put("/api/v1/urls/" + code).with(user(uid)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearExpiresAt\":true}"))
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
    }

    @Test
    void scheduledLinksStartLater() throws Exception {
        String uid = unique("sch-");
        Instant startsAt = CLOCK.instant().plus(Duration.ofHours(3));
        String code = create(uid, Map.of("originalUrl", "https://example.com/launch", "startsAt", startsAt.toString()))
                .path("shortCode").asText();

        mvc.perform(get("/" + code)).andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not active yet")));
        mvc.perform(get("/api/v1/urls?status=scheduled").with(user(uid))).andExpect(jsonPath("$.data.totalItems").value(1));

        CLOCK.advance(Duration.ofHours(3));
        assertThat(redirectStatus(code)).isEqualTo(302);
    }

    @Test
    void clickLimitStopsTheLink() throws Exception {
        String uid = unique("lim-");
        String code = create(uid, Map.of("originalUrl", "https://example.com/limited", "maxClicks", 2))
                .path("shortCode").asText();

        assertThat(redirectStatus(code)).isEqualTo(302);
        assertThat(redirectStatus(code)).isEqualTo(302);
        awaitClicks(code, 2);
        mvc.perform(get("/" + code)).andExpect(status().isGone())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no longer available")));

        mvc.perform(get("/api/v1/urls/summary").with(user(uid)))
                .andExpect(jsonPath("$.data.totalClicks").value(2))
                .andExpect(jsonPath("$.data.expiredLinks").value(1))
                .andExpect(jsonPath("$.data.activeLinks").value(0));

        // Raising the limit reactivates it.
        mvc.perform(put("/api/v1/urls/" + code).with(user(uid)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxClicks\":10}"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(redirectStatus(code)).isEqualTo(302);
    }

    @Test
    void rejectsInvalidLifetimes() throws Exception {
        Instant now = CLOCK.instant();
        Map<String, Object>[] bad = new Map[]{
                Map.of("originalUrl", "https://e.example", "expiresAt", now.plus(Duration.ofMinutes(1)).toString()),
                Map.of("originalUrl", "https://e.example", "expiresAt", now.plus(Duration.ofDays(400)).toString()),
                Map.of("originalUrl", "https://e.example", "expiresAt", now.plus(Duration.ofHours(1)).toString(),
                        "startsAt", now.plus(Duration.ofHours(2)).toString()),
                Map.of("originalUrl", "https://e.example", "maxClicks", 0),
                Map.of("originalUrl", "https://e.example", "expiresAt", "not-a-date"),
        };
        for (Map<String, Object> body : bad) {
            mvc.perform(post("/api/v1/urls/shorten").with(user("validator")).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void anonymousLinksAlwaysExpireWithinThirtyDays() throws Exception {
        JsonNode link = create(null, Map.of("originalUrl", "https://example.com/anon"));
        Instant expiresAt = Instant.parse(link.path("expiresAt").asText());
        assertThat(expiresAt).isEqualTo(CLOCK.instant().plus(Duration.ofDays(30)));

        Map<String, Object> tooLong = new HashMap<>();
        tooLong.put("originalUrl", "https://example.com/anon-long");
        tooLong.put("expiresAt", CLOCK.instant().plus(Duration.ofDays(31)).toString());
        mvc.perform(post("/api/v1/urls/shorten").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("30 days")));

        // Signed-in users may create permanent links.
        assertThat(create(unique("perm-"), Map.of("originalUrl", "https://example.com/forever")).path("expiresAt").isNull()).isTrue();
    }

    @Test
    void cleanupDeletesOnlyLongExpiredAnonymousLinks() throws Exception {
        String alias = unique("anon");
        create(null, Map.of("originalUrl", "https://example.com/temp", "customAlias", alias,
                "expiresAt", CLOCK.instant().plus(Duration.ofHours(1)).toString()));
        String owner = unique("own-");
        String owned = create(owner, Map.of("originalUrl", "https://example.com/mine",
                "expiresAt", CLOCK.instant().plus(Duration.ofHours(1)).toString())).path("shortCode").asText();

        CLOCK.advance(Duration.ofDays(29));
        cleanupJob.purge();
        assertThat(urlRepository.existsByShortCode(alias)).isTrue(); // expired, but not for 30 days yet

        CLOCK.advance(Duration.ofDays(2));
        cleanupJob.purge();
        assertThat(urlRepository.existsByShortCode(alias)).isFalse();   // anonymous: deleted, alias freed
        assertThat(urlRepository.existsByShortCode(owned)).isTrue();    // owned: kept

        // The freed alias can be used again.
        create(unique("reuse-"), Map.of("originalUrl", "https://example.com/new-owner", "customAlias", alias));
    }

    @Test
    void bulkDeleteRemovesOnlyExpiredLinks() throws Exception {
        String uid = unique("bulk-");
        String soon = create(uid, Map.of("originalUrl", "https://example.com/a",
                "expiresAt", CLOCK.instant().plus(Duration.ofMinutes(10)).toString())).path("shortCode").asText();
        String keep = create(uid, Map.of("originalUrl", "https://example.com/b")).path("shortCode").asText();
        CLOCK.advance(Duration.ofMinutes(11));

        mvc.perform(delete("/api/v1/urls?status=all").with(user(uid))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/urls?status=expired").with(user(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(1));
        assertThat(urlRepository.existsByShortCode(soon)).isFalse();
        assertThat(urlRepository.existsByShortCode(keep)).isTrue();
    }

    @Test
    void expiringSortPutsSoonestFirst() throws Exception {
        String uid = unique("sort-");
        String never = create(uid, Map.of("originalUrl", "https://example.com/never")).path("shortCode").asText();
        String later = create(uid, Map.of("originalUrl", "https://example.com/later",
                "expiresAt", CLOCK.instant().plus(Duration.ofDays(5)).toString())).path("shortCode").asText();
        String sooner = create(uid, Map.of("originalUrl", "https://example.com/sooner",
                "expiresAt", CLOCK.instant().plus(Duration.ofHours(2)).toString())).path("shortCode").asText();

        mvc.perform(get("/api/v1/urls?sort=expiring").with(user(uid)))
                .andExpect(jsonPath("$.data.items[0].shortCode").value(sooner))
                .andExpect(jsonPath("$.data.items[1].shortCode").value(later))
                .andExpect(jsonPath("$.data.items[2].shortCode").value(never));
    }

    @Test
    void unknownCodesGetAFriendlyPage() throws Exception {
        mvc.perform(get("/no-such-code-<script>"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Link not found")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))));
    }
}
