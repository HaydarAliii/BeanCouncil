package com.llmcouncil.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Ayarlar/model proxy endpoint'lerindeki beklenen hata durumlarını anlamlı HTTP kodlarına çevirir.
 * Hiçbir durumda istek/yanıt header'ları (ör. Authorization) buradan dışarı sızmaz — sadece
 * exception mesajı döner.
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
