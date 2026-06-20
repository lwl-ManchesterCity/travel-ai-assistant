package com.lwl.travelassistant.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.LlmClient;
import com.lwl.travelassistant.config.TravelLlmProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final TravelLlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(TravelLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank()
                && properties.getBaseUrl() != null
                && !properties.getBaseUrl().isBlank()
                && properties.getModel() != null
                && !properties.getModel().isBlank();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .path(resolveChatCompletionsPath(properties.getBaseUrl()))
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        String requestBody = objectMapper.writeValueAsString(new ChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)
                ),
                properties.getTemperature()
        ));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("LLM 请求失败: HTTP " + response.statusCode() + " -> " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractContent(root.path("choices").path(0).path("message").path("content"));
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM 未返回有效内容");
        }
        return stripMarkdownCodeFence(content);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String resolveChatCompletionsPath(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        return normalized.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions";
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return null;
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText(""))) {
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private String stripMarkdownCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private static class ChatCompletionRequest {

        private final String model;
        private final List<ChatMessage> messages;
        private final double temperature;

        private ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }

        public String getModel() {
            return model;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    private static class ChatMessage {

        private final String role;
        private final String content;

        private ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
