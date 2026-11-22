package com.example.shortener.service;

import java.util.Locale;
import java.util.regex.Pattern;

/** Coarse user-agent classification for click analytics. */
public final class UserAgents {

    /** Crawlers, link-preview fetchers (chat apps, social networks) and scripted HTTP clients. */
    private static final Pattern NON_HUMAN = Pattern.compile(
            "bot|crawl|spider|slurp|preview|facebookexternalhit|whatsapp|telegram|discord|slack|skype"
                    + "|embedly|vkshare|pinterest|headless|lighthouse|curl/|wget/|python-requests|okhttp"
                    + "|java/|go-http-client|httpclient|axios/|node-fetch");

    private UserAgents() {}

    public static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        return NON_HUMAN.matcher(userAgent.toLowerCase(Locale.ROOT)).find();
    }

    /** "tablet", "mobile", "desktop" or "unknown". */
    public static String device(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "mobile";
        }
        return "desktop";
    }
}
