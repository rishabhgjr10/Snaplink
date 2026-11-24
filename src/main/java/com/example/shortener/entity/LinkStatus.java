package com.example.shortener.entity;

/** Whether a short link currently redirects, and if not, why. */
public enum LinkStatus {
    /** Redirects normally. */
    ACTIVE,
    /** Has a start time in the future. */
    SCHEDULED,
    /** Its expiry time has passed. */
    EXPIRED,
    /** Reached its maximum number of clicks. */
    LIMIT_REACHED,
    /** Taken down by an admin. */
    DISABLED;

    public boolean redirects() {
        return this == ACTIVE;
    }
}
