package com.lwl.travelassistant.client;

public interface LlmClient {

    boolean isAvailable();

    String chat(String systemPrompt, String userPrompt) throws Exception;
}
