package com.contractiq.identity;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Component
public class AuthRateLimiter {
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n", Long.class);
    private final StringRedisTemplate redis;

    public AuthRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String key, long limit, Duration window) {
        final Long attempts;
        try {
            attempts = redis.execute(INCREMENT_WITH_TTL, List.of("auth-limit:" + fingerprint(key)), Long.toString(window.toSeconds()));
        } catch (Exception exception) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Authentication rate limiter is unavailable");
        }
        if (attempts == null) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Authentication rate limiter is unavailable");
        if (attempts > limit) throw new ResponseStatusException(TOO_MANY_REQUESTS, "Too many authentication attempts");
    }

    private String fingerprint(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
