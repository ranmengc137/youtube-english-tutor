package com.example.youtubeenglishtutor.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request has an anonymous learner id cookie.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LearnerCookieFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LearnerCookieFilter.class);
    public static final String COOKIE_NAME = "learner_id";

    private final LearnerContext learnerContext;

    public LearnerCookieFilter(LearnerContext learnerContext) {
        this.learnerContext = learnerContext;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String learnerId = resolveOrCreateLearnerId(request, response);
        learnerContext.setCurrentLearnerId(learnerId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            learnerContext.clear();
        }
    }

    private String resolveOrCreateLearnerId(HttpServletRequest request, HttpServletResponse response) {
        String existing = extractLearnerId(request);
        if (existing != null) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, generated)
                .httpOnly(true)
                .secure(false) // set to true when HTTPS is enforced
                .sameSite("Lax")
                .maxAge(Duration.ofDays(365))
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        // Issue once per new visitor; persisted for a year.
        log.debug("Issued new learner_id cookie");
        return generated;
    }

    private String extractLearnerId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        Optional<Cookie> learnerCookie = Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .findFirst();
        if (learnerCookie.isPresent()) {
            String value = learnerCookie.get().getValue();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
