package com.example.shortener.controller;

import com.example.shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Root-level redirect controller.
 * Handles GET /{shortCode} at the application root so that
 * short URLs like http://localhost:8080/abc123 actually work.
 */
@RestController
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable(value = "shortCode") String shortCode,
                                         HttpServletRequest request) {
        var originalOpt = urlService.resolveOriginalUrl(shortCode);
        if (originalOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        urlService.recordClick(shortCode, ip, ua, referer);
        return ResponseEntity.status(302).header("Location", originalOpt.get()).build();
    }
}
