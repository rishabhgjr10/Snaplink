package com.example.shortener.security;

import com.example.shortener.config.AppProperties;
import com.example.shortener.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the Firebase UID of the caller from the verified JWT.
 */
@Component
public class CurrentUser {

    private final AppProperties props;

    public CurrentUser(AppProperties props) {
        this.props = props;
    }

    /** UID of the signed-in caller, or empty for anonymous requests. */
    public Optional<String> uid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            String sub = jwt.getToken().getSubject();
            if (sub != null && !sub.isBlank()) {
                return Optional.of(sub);
            }
        }
        return Optional.empty();
    }

    /** UID of the signed-in caller; 401 when anonymous. */
    public String requireUid() {
        return uid().orElseThrow(ApiException.Unauthorized::new);
    }

    public boolean isAdmin(String uid) {
        return uid != null && props.getAdminUids().stream().map(String::trim).anyMatch(uid::equals);
    }

    /** UID of the signed-in admin; 401 when anonymous, 403 when not an admin. */
    public String requireAdmin() {
        String uid = requireUid();
        if (!isAdmin(uid)) {
            throw new ApiException.Forbidden();
        }
        return uid;
    }
}
