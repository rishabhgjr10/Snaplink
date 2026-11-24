package com.example.shortener.controller;

import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small self-contained HTML pages shown to visitors when a short link does not redirect.
 * Served by the backend so they work even if the frontend is unavailable; every dynamic
 * value is HTML-escaped. Links are relative, so on bauna.xyz "/" is the SnapLink home page.
 */
final class LinkPages {

    private static final DateTimeFormatter UTC_TIME =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private LinkPages() {}

    static String notFound(String code) {
        return page("Link not found", "🔍", "Link not found",
                "There is no SnapLink short link <code>/" + esc(code) + "</code>. Check the address for typos.",
                false, code);
    }

    static String expired(String code) {
        return page("Link expired", "⌛", "This link has expired",
                "The short link <code>/" + esc(code) + "</code> was set to stop working after a certain time, "
                        + "and that time has passed.", true, code);
    }

    static String limitReached(String code) {
        return page("Link no longer available", "🔒", "This link is no longer available",
                "The short link <code>/" + esc(code) + "</code> could only be opened a limited number of times, "
                        + "and that limit has been reached.", true, code);
    }

    static String scheduled(String code, Instant startsAt) {
        String when = startsAt == null ? "soon" : esc(UTC_TIME.format(startsAt));
        return page("Link not active yet", "⏳", "This link is not active yet",
                "The short link <code>/" + esc(code) + "</code> starts working on <strong>" + when + "</strong>. "
                        + "Please come back then.", true, code);
    }

    static String disabled(String code) {
        return page("Link disabled", "⛔", "This link has been disabled",
                "The short link <code>/" + esc(code) + "</code> was disabled because it violated our terms of use.",
                false, code);
    }

    private static String page(String title, String icon, String heading, String bodyHtml, boolean reportLink, String code) {
        String report = reportLink
                ? "<a class=\"ghost\" href=\"/report?code=" + esc(urlEncode(code)) + "\">Report this link</a>"
                : "";
        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="robots" content="noindex">
                <title>%s · SnapLink</title>
                <style>
                  *{box-sizing:border-box;margin:0}
                  body{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:16px;
                       background:#07060f;color:#f0ecff;font-family:system-ui,-apple-system,'Segoe UI',sans-serif}
                  main{max-width:480px;width:100%%;text-align:center;padding:36px 28px;border-radius:18px;
                       background:rgba(255,255,255,.04);border:1px solid rgba(108,99,255,.25)}
                  .icon{font-size:44px;line-height:1;margin-bottom:16px}
                  h1{font-size:24px;margin-bottom:12px}
                  p{color:#a89fc0;line-height:1.6;font-size:15px}
                  code{color:#f0ecff;background:rgba(255,255,255,.08);padding:1px 6px;border-radius:6px}
                  .actions{display:flex;gap:10px;justify-content:center;flex-wrap:wrap;margin-top:26px}
                  a{display:inline-block;padding:10px 18px;border-radius:9px;text-decoration:none;font-size:14px}
                  .primary{background:#6C63FF;color:#fff}
                  .ghost{border:1px solid rgba(255,255,255,.15);color:#f0ecff}
                  .brand{margin-top:22px;font-size:12px;color:#5a5070}
                </style>
                </head>
                <body>
                <main>
                  <div class="icon" aria-hidden="true">%s</div>
                  <h1>%s</h1>
                  <p>%s</p>
                  <div class="actions"><a class="primary" href="/">Go to SnapLink</a>%s</div>
                  <div class="brand">SnapLink by bauna.xyz</div>
                </main>
                </body>
                </html>
                """.formatted(esc(title), icon, esc(heading), bodyHtml, report);
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
