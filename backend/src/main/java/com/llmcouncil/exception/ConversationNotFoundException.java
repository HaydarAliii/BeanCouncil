package com.llmcouncil.exception;

/** İstenen id'de bir konuşma kaydı bulunamadığında fırlatılır. */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
