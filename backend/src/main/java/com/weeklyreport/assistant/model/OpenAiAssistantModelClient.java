package com.weeklyreport.assistant.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.weeklyreport.assistant.config.AssistantProperties;
import com.weeklyreport.assistant.exception.AssistantProviderException;
import com.weeklyreport.assistant.exception.AssistantUnavailableException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** OpenAI-specific adapter. The application services depend only on AssistantModelClient. */
public final class OpenAiAssistantModelClient implements AssistantModelClient {
    private final OpenAIClient client;
    private final ObjectMapper objectMapper;
    private final AssistantProperties properties;

    public OpenAiAssistantModelClient(
            OpenAIClient client,
            ObjectMapper objectMapper,
            AssistantProperties properties
    ) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AssistantModelResponse generate(AssistantModelRequest request) {
        try {
            StructuredResponseCreateParams<StructuredAnswer> parameters = ResponseCreateParams
                    .builder()
                    .input(createInput(request))
                    .instructions(request.instructions())
                    .model(properties.model())
                    .reasoning(Reasoning.builder()
                            .effort(ReasoningEffort.of(properties.reasoningEffort()))
                            .build())
                    .maxOutputTokens(request.maxOutputTokens())
                    .parallelToolCalls(false)
                    .store(false)
                    .truncation(ResponseCreateParams.Truncation.DISABLED)
                    .text(StructuredAnswer.class)
                    .build();

            StructuredResponse<StructuredAnswer> response = client.responses().create(parameters);
            if (response.error().isPresent()
                    || response.status().filter(ResponseStatus.COMPLETED::equals).isEmpty()) {
                throw new AssistantProviderException();
            }
            StructuredAnswer answer = response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(AssistantProviderException::new);
            Integer inputTokens = response.usage()
                    .map(usage -> safeInt(usage.inputTokens()))
                    .orElse(null);
            Integer outputTokens = response.usage()
                    .map(usage -> safeInt(usage.outputTokens()))
                    .orElse(null);
            return new AssistantModelResponse(
                    answer.answer,
                    answer.sourceKeys,
                    inputTokens,
                    outputTokens
            );
        } catch (AssistantProviderException exception) {
            throw exception;
        } catch (OpenAIInvalidDataException exception) {
            throw new AssistantProviderException(exception);
        } catch (OpenAIException exception) {
            throw new AssistantUnavailableException(exception);
        } catch (RuntimeException exception) {
            throw new AssistantProviderException(exception);
        }
    }

    private String createInput(AssistantModelRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("question", request.question());
        input.put("chatHistory", request.history());
        JsonNode context = objectMapper.readTree(request.contextJson());
        input.put("reportContext", context);
        return objectMapper.writeValueAsString(input);
    }

    private Integer safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /** Public fields let the SDK generate and deserialize a strict JSON schema. */
    public static final class StructuredAnswer {
        public String answer;
        public List<String> sourceKeys;

        public StructuredAnswer() {
        }
    }
}
