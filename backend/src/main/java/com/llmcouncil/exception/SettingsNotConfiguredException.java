package com.llmcouncil.exception;

/** OpenRouter key henüz girilmemiş veya konsey üyesi seçilmemişken konsey/model işlemleri çağrılırsa fırlatılır. */
public class SettingsNotConfiguredException extends RuntimeException {

    public SettingsNotConfiguredException(String message) {
        super(message);
    }
}
