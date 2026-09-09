package com.weeklyreport.assistant.exception;

public class AssistantUnavailableException extends RuntimeException {
    public AssistantUnavailableException() {
        super("The AI assistant is not configured or is temporarily unavailable");
    }

    public AssistantUnavailableException(Throwable cause) {
        super("The AI assistant is not configured or is temporarily unavailable", cause);
    }
}
