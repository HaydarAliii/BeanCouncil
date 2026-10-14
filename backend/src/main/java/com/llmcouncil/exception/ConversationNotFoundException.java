package com.llmcouncil.exception;

/** Thrown when no conversation record exists for the requested id. */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
