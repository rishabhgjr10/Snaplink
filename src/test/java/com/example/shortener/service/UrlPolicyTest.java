package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlPolicyTest {

    private UrlPolicy policy;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("https://short.example");
        props.setSelfHosts(List.of("bauna.xyz"));
        props.setBlockedDomains(List.of("evil.test"));
        policy = new UrlPolicy(props, new SafeBrowsingClient(props, new ObjectMapper()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com/path?q=1#frag",
            "HTTPS://Example.com:8443/a",
            "https://例え.jp/パス",
            "https://[2001:db8::1]/x"})
    void acceptsHttpUrls(String url) {
        assertThat(policy.checkDestination("  " + url + "  ")).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "file:///etc/passwd",
            "ftp://example.com",
            "example.com",
            "https://exa mple.com",
            "https://"})
    void rejectsNonHttpOrMalformed(String url) {
        assertThatThrownBy(() -> policy.checkDestination(url)).isInstanceOf(ApiException.InvalidInput.class);
    }

    @Test
    void rejectsEmbeddedCredentials() {
        assertThatThrownBy(() -> policy.checkDestination("https://google.com@evil.example/login"))
                .isInstanceOf(ApiException.InvalidInput.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void rejectsSelfHostsAndBaseUrlHost() {
        assertThatThrownBy(() -> policy.checkDestination("https://BAUNA.xyz/abc")).isInstanceOf(ApiException.InvalidInput.class);
        assertThatThrownBy(() -> policy.checkDestination("https://short.example/abc")).isInstanceOf(ApiException.InvalidInput.class);
    }

    @Test
    void rejectsBlockedDomainsAndSubdomains() {
        assertThatThrownBy(() -> policy.checkDestination("https://evil.test")).isInstanceOf(ApiException.InvalidInput.class);
        assertThatThrownBy(() -> policy.checkDestination("https://login.evil.test/x")).isInstanceOf(ApiException.InvalidInput.class);
        assertThat(policy.checkDestination("https://notevil.test")).isEqualTo("https://notevil.test");
    }

    @Test
    void rejectsNullBlankAndTooLong() {
        assertThatThrownBy(() -> policy.checkDestination(null)).isInstanceOf(ApiException.InvalidInput.class);
        assertThatThrownBy(() -> policy.checkDestination("   ")).isInstanceOf(ApiException.InvalidInput.class);
        assertThatThrownBy(() -> policy.checkDestination("https://example.com/" + "a".repeat(2048)))
                .isInstanceOf(ApiException.InvalidInput.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "My-Promo_2026", "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"})
    void acceptsValidAliases(String alias) {
        assertThat(policy.checkAlias(alias)).isEqualTo(alias);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "has space", "slash/es", "q?x", "per%cent", "ünï", "abcdefghijklmnopqrstuvwxyz0123456"})
    void rejectsInvalidAliases(String alias) {
        assertThatThrownBy(() -> policy.checkAlias(alias)).isInstanceOf(ApiException.InvalidInput.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "Dashboard", "API", "privacy", "admin", "report"})
    void rejectsReservedAliasesCaseInsensitively(String alias) {
        assertThatThrownBy(() -> policy.checkAlias(alias))
                .isInstanceOf(ApiException.InvalidInput.class)
                .hasMessageContaining("reserved");
    }
}
