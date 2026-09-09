package com.weeklyreport.assistant.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.assistant.dto.AssistantResponse;
import com.weeklyreport.assistant.dto.CreateAssistantResponseRequest;
import com.weeklyreport.assistant.service.AssistantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/manager/assistant-responses")
public class AssistantResponseController {
    private final AssistantService assistantService;

    public AssistantResponseController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    public AssistantResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAssistantResponseRequest request
    ) {
        return assistantService.create(UUID.fromString(jwt.getSubject()), request);
    }
}
