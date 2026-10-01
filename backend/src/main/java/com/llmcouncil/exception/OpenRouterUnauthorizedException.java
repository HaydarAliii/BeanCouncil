package com.llmcouncil.exception;

/** OpenRouter kayıtlı key'i reddettiğinde (401) fırlatılır — frontend'e net "key geçersiz" mesajı vermek için. */
public class OpenRouterUnauthorizedException extends RuntimeException {

    public OpenRouterUnauthorizedException(String message) {
        super(message);
    }
}
