package com.weeklyreport.assistant.exception;

public class AssistantRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public AssistantRateLimitExceededException(long retryAfterSeconds) {
        super("Too many assistant requests. Try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
