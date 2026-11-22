package com.example.shortener.security;

import com.example.shortener.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Determines the caller's IP address behind reverse proxies.
 * <p>
 * Each trusted proxy appends the address it received the request from to
 * {@code X-Forwarded-For}. Entries to the left of those are written by the
 * client and can be forged, so the left-most entry is never trusted. With
 * {@code app.rate-limit.trusted-proxy-hops = n}, the address is the entry
 * {@code n} positions from the right (clamped to the first entry).
 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final int trustedHops;

    public ClientIpResolver(AppProperties props) {
        this.trustedHops = Math.max(0, props.getRateLimit().getTrustedProxyHops());
    }

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            int index = Math.max(0, hops.length - 1 - trustedHops);
            String candidate = hops[index].trim();
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Privacy-reduced form for storage: IPv4 keeps the first three octets (a.b.c.0),
     * IPv6 keeps the /48 prefix. Anything else is not stored.
     */
    public static String anonymize(String ip) {
        if (ip == null) {
            return null;
        }
        String s = ip.trim();
        if (IPV4.matcher(s).matches()) {
            return s.substring(0, s.lastIndexOf('.')) + ".0";
        }
        if (s.contains(":") && IPV6.matcher(s).matches()) {
            try {
                // The pattern guarantees a literal, so no DNS lookup happens here.
                byte[] bytes = InetAddress.getByName(s).getAddress();
                if (bytes.length == 16) {
                    for (int i = 6; i < 16; i++) {
                        bytes[i] = 0;
                    }
                    return InetAddress.getByAddress(bytes).getHostAddress();
                }
                if (bytes.length == 4) { // IPv4-mapped IPv6
                    bytes[3] = 0;
                    return InetAddress.getByAddress(bytes).getHostAddress();
                }
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }
}
