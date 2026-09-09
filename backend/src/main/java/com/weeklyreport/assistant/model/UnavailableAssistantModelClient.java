package com.weeklyreport.assistant.model;

import com.weeklyreport.assistant.exception.AssistantUnavailableException;

public final class UnavailableAssistantModelClient implements AssistantModelClient {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public AssistantModelResponse generate(AssistantModelRequest request) {
        throw new AssistantUnavailableException();
    }
}
