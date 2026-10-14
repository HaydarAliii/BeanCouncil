package com.llmcouncil.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates expected error conditions in the settings/model proxy endpoints into meaningful
 * HTTP status codes. Request/response headers (e.g. Authorization) never leak out through here
 * under any circumstances — only the exception message is returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SettingsNotConfiguredException.class)
    public ResponseEntity<Map<String, String>> handleNotConfigured(SettingsNotConfiguredException ex) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(OpenRouterUnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(OpenRouterUnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
