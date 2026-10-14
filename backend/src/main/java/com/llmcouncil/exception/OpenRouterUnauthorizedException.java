package com.llmcouncil.exception;

/** Thrown when OpenRouter rejects the configured key (401) — lets us give the frontend a clear "key invalid" message. */
public class OpenRouterUnauthorizedException extends RuntimeException {

    public OpenRouterUnauthorizedException(String message) {
        super(message);
    }
}
