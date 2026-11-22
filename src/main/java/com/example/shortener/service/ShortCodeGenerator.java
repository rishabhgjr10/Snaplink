package com.example.shortener.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Random 7-character Base62 codes (62^7, about 3.5 trillion). Unlike the old
 * id-based codes they cannot be guessed or enumerated, and they never collide
 * predictably with a custom alias.
 */
@Component
public class ShortCodeGenerator {

    static final int LENGTH = 7;
    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String next() {
        char[] code = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
