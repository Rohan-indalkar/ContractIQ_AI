package com.contractiq.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError.Response> status(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        ApiError.ErrorCode code = switch (status) {
            case NOT_FOUND -> ApiError.ErrorCode.NOT_FOUND;
            case CONFLICT -> ApiError.ErrorCode.CONFLICT;
            case FORBIDDEN -> ApiError.ErrorCode.FORBIDDEN;
            case UNAUTHORIZED -> ApiError.ErrorCode.UNAUTHORIZED;
            case TOO_MANY_REQUESTS -> ApiError.ErrorCode.TOO_MANY_REQUESTS;
            case PAYLOAD_TOO_LARGE -> ApiError.ErrorCode.PAYLOAD_TOO_LARGE;
            case UNSUPPORTED_MEDIA_TYPE -> ApiError.ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case SERVICE_UNAVAILABLE -> ApiError.ErrorCode.PROVIDER_UNAVAILABLE;
            default -> ApiError.ErrorCode.VALIDATION_ERROR;
        };
        return error(status, code, exception.getReason(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError.Response> invalid(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getField(), e -> e.getDefaultMessage() == null ? "Invalid value" : e.getDefaultMessage(),
                        (first, ignored) -> first));
        return error(HttpStatus.BAD_REQUEST, ApiError.ErrorCode.VALIDATION_ERROR, "Request validation failed", fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError.Response> malformed(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, ApiError.ErrorCode.VALIDATION_ERROR, "Invalid request", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError.Response> unauthorized(Exception exception) {
        return error(HttpStatus.UNAUTHORIZED, ApiError.ErrorCode.UNAUTHORIZED, "Authentication required", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError.Response> forbidden(Exception exception) {
        return error(HttpStatus.FORBIDDEN, ApiError.ErrorCode.FORBIDDEN, "Access denied", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError.Response> conflict(Exception exception) {
        return error(HttpStatus.CONFLICT, ApiError.ErrorCode.CONFLICT, "The request conflicts with existing data", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError.Response> tooLarge(Exception exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, ApiError.ErrorCode.PAYLOAD_TOO_LARGE, "Upload exceeds the configured limit", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError.Response> internal(Exception exception, HttpServletRequest request) {
        StringBuilder exceptionTypes = new StringBuilder();
        Throwable cause = exception;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (depth > 0) exceptionTypes.append("->");
            exceptionTypes.append(cause.getClass().getSimpleName());
        }
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error(
                "Unhandled request failure method={} path={} exceptionTypes={}",
                request.getMethod(), request.getRequestURI(), exceptionTypes);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred", Map.of());
    }

    private ResponseEntity<ApiError.Response> error(HttpStatus status, ApiError.ErrorCode code, String message, Map<String, String> details) {
        return ResponseEntity.status(status).body(new ApiError.Response(ApiError.of(code, message, details, MDC.get("correlationId"))));
    }
}
