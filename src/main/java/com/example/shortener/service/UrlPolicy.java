package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.exception.ApiException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rules for destination URLs and custom aliases.
 */
@Component
public class UrlPolicy {

    static final int MAX_URL_LENGTH = 2048;
    static final Pattern ALIAS = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");
    /** http(s) scheme, then an authority, then an optional path/query/fragment; no whitespace anywhere. */
    private static final Pattern HTTP_URL = Pattern.compile("^(?i)(https?)://([^/?#\\s]+)([/?#]\\S*)?$");

    /** Paths the frontend (vercel.json) or backend serve themselves; such an alias would never redirect. */
    static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "v1", "login", "signup", "logout", "dashboard", "analytics", "settings", "features",
            "pricing", "privacy", "terms", "report", "admin", "assets", "static", "public", "health",
            "actuator", "error", "index", "app", "www", "help", "about", "contact", "robots", "sitemap",
            "favicon", "icon", "expired");

    private final Set<String> selfHosts;
    private final Set<String> blockedDomains;
    private final SafeBrowsingClient safeBrowsing;

    public UrlPolicy(AppProperties props, SafeBrowsingClient safeBrowsing) {
        this.safeBrowsing = safeBrowsing;
        this.selfHosts = Stream.concat(props.getSelfHosts().stream(), Stream.of(hostOf(props.getBaseUrl())))
                .filter(h -> h != null && !h.isBlank())
                .map(h -> h.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.blockedDomains = props.getBlockedDomains().stream()
                .filter(d -> d != null && !d.isBlank())
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns the trimmed destination, or throws 400 explaining why it is not accepted. */
    public String checkDestination(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            throw new ApiException.InvalidInput("Enter a URL.");
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new ApiException.InvalidInput("URL must be at most " + MAX_URL_LENGTH + " characters.");
        }
        Matcher matcher = HTTP_URL.matcher(url);
        if (!matcher.matches()) {
            throw new ApiException.InvalidInput("Only http:// and https:// URLs can be shortened.");
        }
        String authority = matcher.group(2);
        if (authority.contains("@")) {
            // https://trusted.com@evil.com sends the visitor to evil.com
            throw new ApiException.InvalidInput("URLs with embedded credentials are not allowed.");
        }
        String host = stripPort(authority).toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            throw new ApiException.InvalidInput("The URL has no host name.");
        }
        if (selfHosts.contains(host)) {
            throw new ApiException.InvalidInput("Links to this shortener itself are not allowed.");
        }
        if (matchesDomain(host, blockedDomains)) {
            throw new ApiException.InvalidInput("Links to this domain are not allowed.");
        }
        if (safeBrowsing.isUnsafe(url)) {
            throw new ApiException.InvalidInput("This URL is flagged as unsafe by Google Safe Browsing.");
        }
        return url;
    }

    /** Returns the trimmed alias, or throws 400 explaining why it is not accepted. */
    public String checkAlias(String raw) {
        String alias = raw == null ? "" : raw.trim();
        if (!ALIAS.matcher(alias).matches()) {
            throw new ApiException.InvalidInput("Alias must be 3-32 characters: letters, numbers, '-' or '_'.");
        }
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new ApiException.InvalidInput("'" + alias + "' is reserved. Please choose another alias.");
        }
        return alias;
    }

    private static boolean matchesDomain(String host, Set<String> domains) {
        for (String d : domains) {
            if (host.equals(d) || host.endsWith("." + d)) {
                return true;
            }
        }
        return false;
    }

    private static String stripPort(String authority) {
        if (authority.startsWith("[")) { // IPv6 literal
            int end = authority.indexOf(']');
            return end > 0 ? authority.substring(1, end) : authority;
        }
        int colon = authority.lastIndexOf(':');
        return colon >= 0 ? authority.substring(0, colon) : authority;
    }

    private static String hostOf(String url) {
        try {
            return url == null || url.isBlank() ? null : URI.create(url.trim()).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
