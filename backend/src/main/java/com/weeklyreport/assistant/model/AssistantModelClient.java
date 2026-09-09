package com.weeklyreport.assistant.model;

public interface AssistantModelClient {

    boolean isAvailable();

    AssistantModelResponse generate(AssistantModelRequest request);
}
