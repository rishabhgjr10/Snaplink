package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ClickFilteringTest {

    private static final String CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36";
    private static final String IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148";

    @ParameterizedTest
    @ValueSource(strings = {
            "Googlebot/2.1 (+http://www.google.com/bot.html)",
            "WhatsApp/2.23.20.0",
            "Slackbot-LinkExpanding 1.0 (+https://api.slack.com/robots)",
            "facebookexternalhit/1.1",
            "TelegramBot (like TwitterBot)",
            "curl/8.4.0",
            "python-requests/2.31",
            ""})
    void classifiesNonHumanAgents(String ua) {
        assertThat(UserAgents.isBot(ua)).isTrue();
    }

    @Test
    void classifiesBrowsers() {
        assertThat(UserAgents.isBot(CHROME)).isFalse();
        assertThat(UserAgents.device(CHROME)).isEqualTo("desktop");
        assertThat(UserAgents.device(IPHONE)).isEqualTo("mobile");
        assertThat(UserAgents.device("Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X)")).isEqualTo("tablet");
        assertThat(UserAgents.device(null)).isEqualTo("unknown");
    }

    @Test
    void countsRepeatClicksOncePerWindowAndIgnoresBots() {
        ClickRecorder recorder = new ClickRecorder(mock(com.example.shortener.repository.UrlRepository.class),
                mock(com.example.shortener.repository.UrlAnalyticsRepository.class), new AppProperties());

        assertThat(recorder.shouldCount("abc", "1.2.3.4", CHROME)).isTrue();
        assertThat(recorder.shouldCount("abc", "1.2.3.4", CHROME)).isFalse();
        assertThat(recorder.shouldCount("abc", "1.2.3.5", CHROME)).isTrue();
        assertThat(recorder.shouldCount("xyz", "1.2.3.4", CHROME)).isTrue();
        assertThat(recorder.shouldCount("abc", "9.9.9.9", "WhatsApp/2.0")).isFalse();
    }

    @Test
    void keepsOnlyRefererOrigin() {
        assertThat(ClickRecorder.refererOrigin("https://Mail.Example.com/inbox/123?token=secret"))
                .isEqualTo("https://mail.example.com");
        assertThat(ClickRecorder.refererOrigin("http://localhost:5173/x")).isEqualTo("http://localhost:5173");
        assertThat(ClickRecorder.refererOrigin("not a url")).isNull();
        assertThat(ClickRecorder.refererOrigin(null)).isNull();
    }
}
