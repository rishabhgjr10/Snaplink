package com.example.shortener.controller;

import com.example.shortener.security.ClientIpResolver;
import com.example.shortener.service.ClickRecorder;
import com.example.shortener.service.UrlService;
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

import java.time.Instant;
import java.util.Optional;

/**
 * Root-level redirect: GET /{shortCode} answers 302 to the destination.
 * The click is recorded asynchronously; HEAD requests are answered but never counted.
 */
@RestController
public class RedirectController {

    private final UrlService urlService;
    private final ClickRecorder clickRecorder;
    private final ClientIpResolver ipResolver;

    public RedirectController(UrlService urlService, ClickRecorder clickRecorder, ClientIpResolver ipResolver) {
        this.urlService = urlService;
        this.clickRecorder = clickRecorder;
        this.ipResolver = ipResolver;
    }

    @RequestMapping(value = "/{shortCode}", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> redirect(@PathVariable("shortCode") String shortCode, HttpServletRequest request) {
        Optional<String> destination;
        try {
            destination = urlService.resolve(shortCode);
        } catch (UrlService.LinkGone gone) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("This link has been disabled because it violated our terms.");
        }
        if (destination.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            clickRecorder.record(shortCode, Instant.now(), ipResolver.resolve(request),
                    request.getHeader(HttpHeaders.USER_AGENT), request.getHeader(HttpHeaders.REFERER));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, destination.get())
                // Browsers must come back each time, so every click is counted and takedowns apply at once.
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
