package com.example.shortener.controller;

import com.example.shortener.dto.LinkSummary;
import com.example.shortener.dto.MeResponse;
import com.example.shortener.dto.PageResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlUpdateRequest;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.service.UrlService;
import com.example.shortener.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    private final UrlService urlService;
    private final CurrentUser currentUser;

    public UrlController(UrlService urlService, CurrentUser currentUser) {
        this.urlService = urlService;
        this.currentUser = currentUser;
    }

    /** Anonymous callers may create links; signed-in callers become the link's owner. */
    @PostMapping("/urls/shorten")
    public ResponseEntity<ApiResponse<UrlShortenResponse>> shorten(@Valid @RequestBody UrlShortenRequest request) {
        UrlShortenResponse resp = urlService.shorten(request, currentUser.uid().orElse(null));
        return ResponseEntity.ok(ApiResponse.success(resp, "URL shortened", 200));
    }

    @GetMapping("/urls")
    public ResponseEntity<ApiResponse<PageResponse<UrlItemDto>>> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        PageResponse<UrlItemDto> result = urlService.list(currentUser.requireUid(), q, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "URLs retrieved", 200));
    }

    @GetMapping("/urls/summary")
    public ResponseEntity<ApiResponse<LinkSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.success(urlService.summary(currentUser.requireUid()), "Summary", 200));
    }

    @PutMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<UrlItemDto>> update(@PathVariable("shortCode") String shortCode,
                                                          @Valid @RequestBody UrlUpdateRequest request) {
        UrlItemDto updated = urlService.update(currentUser.requireUid(), shortCode, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "URL updated successfully", 200));
    }

    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("shortCode") String shortCode) {
        urlService.delete(currentUser.requireUid(), shortCode);
        return ResponseEntity.ok(ApiResponse.success(null, "URL deleted successfully", 200));
    }

    @GetMapping("/urls/{shortCode}/analytics")
    public ResponseEntity<ApiResponse<UrlAnalyticsResponse>> analytics(@PathVariable("shortCode") String shortCode) {
        UrlAnalyticsResponse resp = urlService.analytics(currentUser.requireUid(), shortCode);
        return ResponseEntity.ok(ApiResponse.success(resp, "Analytics fetched", 200));
    }

    /** Who the caller is, and whether they may use the admin endpoints. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me() {
        String uid = currentUser.requireUid();
        return ResponseEntity.ok(ApiResponse.success(new MeResponse(uid, currentUser.isAdmin(uid)), "OK", 200));
    }
}
