package com.llmcouncil.exception;

/** Thrown when a council/model operation is called before an OpenRouter key has been entered or any council members selected. */
public class SettingsNotConfiguredException extends RuntimeException {

    public SettingsNotConfiguredException(String message) {
        super(message);
    }
}
