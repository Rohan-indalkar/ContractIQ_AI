package com.contractiq.shared;

import java.time.Instant;
import java.util.Map;

public record ApiError(ErrorCode code, String message, Map<String, String> details, String correlationId, Instant timestamp) {
    public enum ErrorCode {
        VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, TOO_MANY_REQUESTS,
        UNSUPPORTED_MEDIA_TYPE, PAYLOAD_TOO_LARGE, PROVIDER_UNAVAILABLE, INTERNAL_ERROR
    }

    public static ApiError of(ErrorCode code, String message, Map<String, String> details, String correlationId) {
        return new ApiError(code, message, details, correlationId, Instant.now());
    }

    public record Response(ApiError error) {}
}
