package com.weeklyreport.assistant.exception;

public class AssistantProviderException extends RuntimeException {
    public AssistantProviderException() {
        super("The AI assistant returned an invalid response");
    }

    public AssistantProviderException(Throwable cause) {
        super("The AI assistant returned an invalid response", cause);
    }
}
