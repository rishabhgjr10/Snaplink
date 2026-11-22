package com.example.shortener.dto;

/** The public short-link URL is built by the client from its configured short-link origin. */
public record UrlShortenResponse(String shortCode, String originalUrl) {
}
