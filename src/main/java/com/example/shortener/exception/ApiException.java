package com.example.shortener.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors whose HTTP status is part of the API contract.
 * GlobalExceptionHandler maps it by type, never by message text.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 404 — the link does not exist or belongs to someone else. */
    public static class LinkNotFound extends ApiException {
        public LinkNotFound() {
            super(HttpStatus.NOT_FOUND, "Link not found.");
        }
    }

    /** 409 — the requested short code is taken. */
    public static class AliasConflict extends ApiException {
        public AliasConflict(String alias) {
            super(HttpStatus.CONFLICT, "Custom alias '" + alias + "' is already in use. Please choose another.");
        }

        public AliasConflict() {
            super(HttpStatus.CONFLICT, "That short code is already in use. Please try again.");
        }
    }

    /** 400 — the destination URL or alias is not acceptable. */
    public static class InvalidInput extends ApiException {
        public InvalidInput(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }
    }

    /** 403 — authenticated but not allowed. */
    public static class Forbidden extends ApiException {
        public Forbidden() {
            super(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
        }
    }

    /** 401 — the operation needs a signed-in user. */
    public static class Unauthorized extends ApiException {
        public Unauthorized() {
            super(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
        }
    }
}
