package com.example.shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed view of the {@code app.*} settings in application.properties.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Public origin that short links are served from. */
    private String baseUrl = "http://localhost:8080";
    /** Hosts that must never be used as a destination (redirect loops). */
    private List<String> selfHosts = new ArrayList<>();
    /** Destination domains rejected on create/update; subdomains included. */
    private List<String> blockedDomains = new ArrayList<>();
    /** Firebase UIDs allowed to use the admin endpoints. */
    private List<String> adminUids = new ArrayList<>();
    /** Firebase project whose ID tokens are accepted. */
    private String firebaseProjectId = "snapliink";
    /** Optional Google Safe Browsing API key. */
    private String safeBrowsingApiKey = "";

    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();
    private final Analytics analytics = new Analytics();
    private final Cache cache = new Cache();
    private final Links links = new Links();

    /** Limits on link lifetime (expiry, start time, click limit). */
    public static class Links {
        /** Shortest allowed time until expiry. */
        private int minLifetimeMinutes = 5;
        /** Longest allowed expiry or start time from now. */
        private int maxLifetimeDays = 365;
        /** Links created without signing in expire after at most this many days (also their default). */
        private int anonymousMaxDays = 30;
        /** Anonymous links are deleted this many days after they expire, freeing their alias. */
        private int anonymousExpiredRetentionDays = 30;

        public int getMinLifetimeMinutes() { return minLifetimeMinutes; }
        public void setMinLifetimeMinutes(int v) { this.minLifetimeMinutes = v; }
        public int getMaxLifetimeDays() { return maxLifetimeDays; }
        public void setMaxLifetimeDays(int v) { this.maxLifetimeDays = v; }
        public int getAnonymousMaxDays() { return anonymousMaxDays; }
        public void setAnonymousMaxDays(int v) { this.anonymousMaxDays = v; }
        public int getAnonymousExpiredRetentionDays() { return anonymousExpiredRetentionDays; }
        public void setAnonymousExpiredRetentionDays(int v) { this.anonymousExpiredRetentionDays = v; }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class RateLimit {
        private int writePerMinute = 20;
        private int readPerMinute = 120;
        private int anonymousGlobalPerMinute = 200;
        private int trustedProxyHops = 1;

        public int getWritePerMinute() { return writePerMinute; }
        public void setWritePerMinute(int writePerMinute) { this.writePerMinute = writePerMinute; }
        public int getReadPerMinute() { return readPerMinute; }
        public void setReadPerMinute(int readPerMinute) { this.readPerMinute = readPerMinute; }
        public int getAnonymousGlobalPerMinute() { return anonymousGlobalPerMinute; }
        public void setAnonymousGlobalPerMinute(int anonymousGlobalPerMinute) { this.anonymousGlobalPerMinute = anonymousGlobalPerMinute; }
        public int getTrustedProxyHops() { return trustedProxyHops; }
        public void setTrustedProxyHops(int trustedProxyHops) { this.trustedProxyHops = trustedProxyHops; }
    }

    public static class Analytics {
        private int retentionDays = 365;
        private int dedupeWindowSeconds = 30;

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public int getDedupeWindowSeconds() { return dedupeWindowSeconds; }
        public void setDedupeWindowSeconds(int dedupeWindowSeconds) { this.dedupeWindowSeconds = dedupeWindowSeconds; }
    }

    public static class Cache {
        private int ttlHours = 24;
        private int localMaxEntries = 10_000;

        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
        public int getLocalMaxEntries() { return localMaxEntries; }
        public void setLocalMaxEntries(int localMaxEntries) { this.localMaxEntries = localMaxEntries; }
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public List<String> getSelfHosts() { return selfHosts; }
    public void setSelfHosts(List<String> selfHosts) { this.selfHosts = selfHosts; }
    public List<String> getBlockedDomains() { return blockedDomains; }
    public void setBlockedDomains(List<String> blockedDomains) { this.blockedDomains = blockedDomains; }
    public List<String> getAdminUids() { return adminUids; }
    public void setAdminUids(List<String> adminUids) { this.adminUids = adminUids; }
    public String getFirebaseProjectId() { return firebaseProjectId; }
    public void setFirebaseProjectId(String firebaseProjectId) { this.firebaseProjectId = firebaseProjectId; }
    public String getSafeBrowsingApiKey() { return safeBrowsingApiKey; }
    public void setSafeBrowsingApiKey(String safeBrowsingApiKey) { this.safeBrowsingApiKey = safeBrowsingApiKey; }
    public Cors getCors() { return cors; }
    public RateLimit getRateLimit() { return rateLimit; }
    public Analytics getAnalytics() { return analytics; }
    public Cache getCache() { return cache; }
    public Links getLinks() { return links; }
}
