package com.example.shortener;

import com.example.shortener.repository.UrlAnalyticsRepository;
import com.example.shortener.repository.UrlRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest extends MySqlContainerSupport {

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0 Safari/537.36";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UrlRepository urlRepository;
    @Autowired UrlAnalyticsRepository analyticsRepository;

    private static RequestPostProcessor user(String uid) {
        return jwt().jwt(j -> j.subject(uid));
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private MockHttpServletRequestBuilder jsonPost(String path, Object body) throws Exception {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    }

    private String create(String uid, String url, String alias) throws Exception {
        var req = jsonPost("/api/v1/urls/shorten", alias == null
                ? Map.of("originalUrl", url)
                : Map.of("originalUrl", url, "customAlias", alias));
        if (uid != null) {
            req = req.with(user(uid));
        }
        String body = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data").path("shortCode").asText();
    }

    /** A click from a new visitor each time (distinct IP), so de-duplication does not merge them. */
    private void click(String code) throws Exception {
        String ip = "198.51.100." + VISITOR.incrementAndGet() % 250;
        mvc.perform(get("/" + code).header("User-Agent", BROWSER_UA).with(r -> { r.setRemoteAddr(ip); return r; }))
                .andExpect(status().isFound());
    }

    private static final java.util.concurrent.atomic.AtomicInteger VISITOR = new java.util.concurrent.atomic.AtomicInteger();

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("condition not met within 5s");
            }
            Thread.sleep(50);
        }
    }

    private long clicks(String code) {
        return urlRepository.findByShortCode(code).map(analyticsRepository::countByUrl).orElse(-1L);
    }

    // ------------------------------------------------------------------ authentication

    @Test
    void managementEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/v1/urls")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(put("/api/v1/urls/abc").contentType(MediaType.APPLICATION_JSON).content("{\"originalUrl\":\"https://x.example\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/urls/abc")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/urls/abc/analytics")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        String code = create(null, "https://example.com/anon", null);
        assertThat(code).hasSize(7);
        mvc.perform(get("/" + code)).andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/anon"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // ------------------------------------------------------------------ ownership

    @Test
    void usersOnlySeeAndChangeTheirOwnLinks() throws Exception {
        String alice = unique("alice-");
        String bob = unique("bob-");
        String code = create(alice, "https://example.com/alice", null);
        create(null, "https://example.com/anonymous", null);

        mvc.perform(get("/api/v1/urls").with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].shortCode").value(code));
        mvc.perform(get("/api/v1/urls").with(user(bob)))
                .andExpect(jsonPath("$.data.totalItems").value(0));

        // Bob cannot read, edit, delete or inspect Alice's link: it looks missing.
        mvc.perform(put("/api/v1/urls/" + code).with(user(bob)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://phish.example\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/urls/" + code).with(user(bob))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/urls/" + code + "/analytics").with(user(bob))).andExpect(status().isNotFound());

        mvc.perform(get("/" + code)).andExpect(header().string("Location", "https://example.com/alice"));
    }

    // ------------------------------------------------------------------ validation

    @Test
    void rejectsUnsafeDestinationsAndBadAliases() throws Exception {
        for (String bad : new String[]{"javascript:alert(1)", "data:text/html,x", "https://bauna.xyz/loop",
                "https://google.com@evil.example", "not a url"}) {
            mvc.perform(jsonPost("/api/v1/urls/shorten", Map.of("originalUrl", bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(jsonPost("/api/v1/urls/shorten", Map.of("originalUrl", "")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/urls/shorten").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/urls/shorten").contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
        mvc.perform(jsonPost("/api/v1/urls/shorten", Map.of("originalUrl", "https://example.com", "customAlias", "login")))
                .andExpect(status().isBadRequest());
        mvc.perform(jsonPost("/api/v1/urls/shorten", Map.of("originalUrl", "https://example.com", "customAlias", "a/b")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aliasesAreCaseSensitiveAndDuplicatesConflict() throws Exception {
        String base = unique("Case");
        create(null, "https://example.com/upper", base);
        create(null, "https://example.com/lower", base.toLowerCase());

        mvc.perform(get("/" + base)).andExpect(header().string("Location", "https://example.com/upper"));
        mvc.perform(get("/" + base.toLowerCase())).andExpect(header().string("Location", "https://example.com/lower"));

        mvc.perform(jsonPost("/api/v1/urls/shorten", Map.of("originalUrl", "https://example.com/x", "customAlias", base)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ------------------------------------------------------------------ analytics

    @Test
    void clicksSurviveAliasRenameAndExposeNoIp() throws Exception {
        String uid = unique("owner-");
        String code = create(uid, "https://example.com/tracked", null);

        click(code);
        click(code);
        mvc.perform(head("/" + code).header("User-Agent", BROWSER_UA)).andExpect(status().isFound());
        mvc.perform(get("/" + code).header("User-Agent", "WhatsApp/2.23")).andExpect(status().isFound());
        await(() -> clicks(code) == 2);
        Thread.sleep(300); // give an incorrectly counted HEAD or bot request time to show up
        assertThat(clicks(code)).isEqualTo(2);

        String renamed = unique("renamed");
        mvc.perform(put("/api/v1/urls/" + code).with(user(uid)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("customAlias", renamed))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortCode").value(renamed))
                .andExpect(jsonPath("$.data.totalClicks").value(2));
        mvc.perform(get("/" + code)).andExpect(status().isNotFound());

        String body = mvc.perform(get("/api/v1/urls/" + renamed + "/analytics").with(user(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClicks").value(2))
                .andExpect(jsonPath("$.data.clicksTodayByHour.length()").value(24))
                .andExpect(jsonPath("$.data.recentVisits[0].device").value("desktop"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("198.51.100").doesNotContain("ipAddress").doesNotContain("userAgent");

        JsonNode byHour = json.readTree(body).path("data").path("clicksTodayByHour");
        long sum = 0;
        for (JsonNode n : byHour) {
            sum += n.asLong();
        }
        assertThat(sum).isEqualTo(2);

        mvc.perform(get("/api/v1/urls/summary").with(user(uid)))
                .andExpect(jsonPath("$.data.totalLinks").value(1))
                .andExpect(jsonPath("$.data.totalClicks").value(2));
    }

    @Test
    void deletingALinkRemovesItsClicks() throws Exception {
        String uid = unique("del-");
        String code = create(uid, "https://example.com/gone", null);
        click(code);
        await(() -> clicks(code) == 1);
        Long id = urlRepository.findByShortCode(code).orElseThrow().getId();

        mvc.perform(delete("/api/v1/urls/" + code).with(user(uid))).andExpect(status().isOk());
        mvc.perform(get("/" + code)).andExpect(status().isNotFound());
        assertThat(urlRepository.findById(id)).isEmpty();
        assertThat(analyticsRepository.findAll()).noneMatch(a -> a.getUrl().getId().equals(id));
    }

    @Test
    void listSupportsPagingAndSearch() throws Exception {
        String uid = unique("pager-");
        for (int i = 0; i < 5; i++) {
            create(uid, "https://example.com/page-" + i, null);
        }
        create(uid, "https://findme.example/needle", null);

        mvc.perform(get("/api/v1/urls?page=0&size=2").with(user(uid)))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.totalItems").value(6))
                .andExpect(jsonPath("$.data.totalPages").value(3));
        mvc.perform(get("/api/v1/urls?q=NEEDLE").with(user(uid)))
                .andExpect(jsonPath("$.data.totalItems").value(1));
        // LIKE wildcards in the query are literal characters
        mvc.perform(get("/api/v1/urls?q=%25").with(user(uid)))
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    // ------------------------------------------------------------------ abuse handling

    @Test
    void reportsAndAdminTakedown() throws Exception {
        String code = create(null, "https://example.com/suspicious", null);

        mvc.perform(jsonPost("/api/v1/reports", Map.of("shortCode", code, "reason", "PHISHING", "details", "fake bank")))
                .andExpect(status().isOk());
        mvc.perform(jsonPost("/api/v1/reports", Map.of("shortCode", "doesNotExist", "reason", "SPAM")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/admin/reports").with(user(unique("nobody-")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me").with(user("admin-uid"))).andExpect(jsonPath("$.data.admin").value(true));

        String reports = mvc.perform(get("/api/v1/admin/reports").with(user("admin-uid")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long reportId = -1;
        for (JsonNode r : json.readTree(reports).path("data").path("items")) {
            if (r.path("shortCode").asText().equals(code)) {
                reportId = r.path("id").asLong();
                assertThat(r.path("destination").asText()).isEqualTo("https://example.com/suspicious");
            }
        }
        assertThat(reportId).isPositive();

        mvc.perform(get("/" + code)).andExpect(status().isFound()); // warms the cache
        mvc.perform(post("/api/v1/admin/reports/" + reportId + "/resolve?status=RESOLVED&disableLink=true")
                        .with(user("admin-uid")))
                .andExpect(status().isOk());
        mvc.perform(get("/" + code)).andExpect(status().isGone());

        mvc.perform(post("/api/v1/admin/urls/" + code + "/enable").with(user("admin-uid"))).andExpect(status().isOk());
        mvc.perform(get("/" + code)).andExpect(status().isFound());
    }
}
