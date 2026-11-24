package com.example.shortener.controller;

import com.example.shortener.security.ClientIpResolver;
import com.example.shortener.service.ClickRecorder;
import com.example.shortener.service.UrlService;
import com.example.shortener.service.UrlService.Resolution;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Optional;

/**
 * Root-level redirect: GET /{shortCode} answers 302 to the destination.
 * Links that do not redirect get a small explanatory page instead:
 * 404 unknown, 410 expired / click limit reached / disabled, 403 not active yet.
 * The click is recorded asynchronously; HEAD requests are answered but never counted.
 */
@RestController
public class RedirectController {

    private final UrlService urlService;
    private final ClickRecorder clickRecorder;
    private final ClientIpResolver ipResolver;
    private final Clock clock;

    public RedirectController(UrlService urlService, ClickRecorder clickRecorder, ClientIpResolver ipResolver, Clock clock) {
        this.urlService = urlService;
        this.clickRecorder = clickRecorder;
        this.ipResolver = ipResolver;
        this.clock = clock;
    }

    @RequestMapping(value = "/{shortCode}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> redirect(@PathVariable("shortCode") String shortCode, HttpServletRequest request) {
        Optional<Resolution> found = urlService.resolve(shortCode);
        if (found.isEmpty()) {
            return page(HttpStatus.NOT_FOUND, LinkPages.notFound(shortCode));
        }
        Resolution r = found.get();
        return switch (r.status()) {
            case ACTIVE -> {
                if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
                    clickRecorder.record(shortCode, clock.instant(), ipResolver.resolve(request),
                            request.getHeader(HttpHeaders.USER_AGENT), request.getHeader(HttpHeaders.REFERER));
                }
                yield ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, r.url())
                        // Browsers must come back each time, so every click is counted and limits apply at once.
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
            case EXPIRED -> page(HttpStatus.GONE, LinkPages.expired(shortCode));
            case LIMIT_REACHED -> page(HttpStatus.GONE, LinkPages.limitReached(shortCode));
            case DISABLED -> page(HttpStatus.GONE, LinkPages.disabled(shortCode));
            case SCHEDULED -> page(HttpStatus.FORBIDDEN, LinkPages.scheduled(shortCode, r.startsAt()));
        };
    }

    private static ResponseEntity<String> page(HttpStatus status, String html) {
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex")
                .body(html);
    }
}
