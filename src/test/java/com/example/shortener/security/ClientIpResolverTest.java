package com.example.shortener.security;

import com.example.shortener.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static ClientIpResolver resolver(int hops) {
        AppProperties props = new AppProperties();
        props.getRateLimit().setTrustedProxyHops(hops);
        return new ClientIpResolver(props);
    }

    private static MockHttpServletRequest request(String xff, String remote) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        req.setRemoteAddr(remote);
        return req;
    }

    @Test
    void ignoresClientForgedLeftmostEntry() {
        // client forged "6.6.6.6"; the real client 203.0.113.7 was appended by the first proxy,
        // and the second (trusted) proxy appended its own address.
        assertThat(resolver(1).resolve(request("6.6.6.6, 203.0.113.7, 10.0.0.2", "10.0.0.3")))
                .isEqualTo("203.0.113.7");
    }

    @Test
    void zeroHopsUsesRightmostEntry() {
        assertThat(resolver(0).resolve(request("6.6.6.6, 198.51.100.4", "10.0.0.3"))).isEqualTo("198.51.100.4");
    }

    @Test
    void clampsToFirstEntryWhenChainIsShort() {
        assertThat(resolver(3).resolve(request("198.51.100.4", "10.0.0.3"))).isEqualTo("198.51.100.4");
    }

    @Test
    void fallsBackToRemoteAddress() {
        assertThat(resolver(1).resolve(request(null, "192.0.2.9"))).isEqualTo("192.0.2.9");
        assertThat(resolver(1).resolve(request("  ", "192.0.2.9"))).isEqualTo("192.0.2.9");
    }

    @Test
    void anonymizesIpv4ToSlash24() {
        assertThat(ClientIpResolver.anonymize("203.0.113.77")).isEqualTo("203.0.113.0");
    }

    @Test
    void anonymizesIpv6ToSlash48() {
        assertThat(ClientIpResolver.anonymize("2001:db8:abcd:12:34::1")).isEqualTo("2001:db8:abcd:0:0:0:0:0");
    }

    @Test
    void dropsAnythingThatIsNotAnIpLiteral() {
        assertThat(ClientIpResolver.anonymize("example.com")).isNull();
        assertThat(ClientIpResolver.anonymize("unknown")).isNull();
        assertThat(ClientIpResolver.anonymize(null)).isNull();
    }
}
