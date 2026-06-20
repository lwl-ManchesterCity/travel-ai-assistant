package com.lwl.travelassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.LlmClient;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RequirementAnalysisService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public RequirementAnalysisService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public PlanningConstraints analyze(TripPlanRequest request) {
        if (llmClient.isAvailable()) {
            try {
                return analyzeByLlm(request);
            } catch (Exception exception) {
                PlanningConstraints fallback = analyzeByRules(request);
                fallback.setAnalysisSource("rule-fallback");
                return fallback;
            }
        }
        return analyzeByRules(request);
    }

    private PlanningConstraints analyzeByLlm(TripPlanRequest request) throws Exception {
        JsonNode result = requestConstraintJson(request);
        PlanningConstraints constraints = new PlanningConstraints(
                result.path("relaxedPace").asBoolean(false),
                result.path("avoidSeafood").asBoolean(false),
                result.path("wantsLandmarkCeremony").asBoolean(false),
                result.path("prefersIndoorBackup").asBoolean(false),
                readStringList(result.path("extractedTags"))
        );
        constraints.setAnalysisSource("llm");
        if (constraints.getExtractedTags() == null || constraints.getExtractedTags().isEmpty()) {
            constraints.setExtractedTags(buildTags(constraints));
        }
        return constraints;
    }

    private JsonNode requestConstraintJson(TripPlanRequest request) throws Exception {
        String content = llmClient.chat(buildSystemPrompt(), buildUserPrompt(request));
        return objectMapper.readTree(content);
    }

    private String buildSystemPrompt() {
        return """
                你是旅行需求理解助手。你的任务是把用户的旅行请求解析成结构化约束 JSON。
                你必须只返回 JSON，不要返回解释、Markdown 或代码块。
                JSON 字段固定为：
                {
                  "relaxedPace": boolean,
                  "avoidSeafood": boolean,
                  "wantsLandmarkCeremony": boolean,
                  "prefersIndoorBackup": boolean,
                  "extractedTags": string[]
                }
                字段含义：
                relaxedPace：用户希望慢节奏、轻松、不要太赶。
                avoidSeafood：用户不吃海鲜、海鲜过敏、避开海鲜。
                wantsLandmarkCeremony：用户提到升旗、仪式感、地标打卡等。
                prefersIndoorBackup：用户担心下雨、高温、天气不稳定，或希望室内备选。
                extractedTags 用简短中文标签总结命中的约束。
                """;
    }

    private String buildUserPrompt(TripPlanRequest request) {
        return "目的地：" + safe(request.getCity()) + "\n"
                + "日期：" + safe(request.getStartDate()) + " 至 " + safe(request.getEndDate()) + "\n"
                + "预算：" + request.getBudget() + "\n"
                + "偏好：" + request.getPreferences() + "\n"
                + "交通：" + safe(request.getTransportation()) + "\n"
                + "住宿：" + safe(request.getAccommodation()) + "\n"
                + "额外要求：" + safe(request.getExtraRequirements());
    }

    private PlanningConstraints analyzeByRules(TripPlanRequest request) {
        String content = request.getExtraRequirements() == null
                ? ""
                : request.getExtraRequirements().toLowerCase(Locale.ROOT);

        boolean relaxedPace = content.contains("轻松") || content.contains("慢") || request.getPreferences().contains("轻松节奏");
        boolean avoidSeafood = content.contains("海鲜过敏") || content.contains("不吃海鲜");
        boolean wantsLandmarkCeremony = content.contains("升旗") || content.contains("仪式");
        boolean prefersIndoorBackup = content.contains("下雨") || content.contains("室内");

        PlanningConstraints constraints = new PlanningConstraints(
                relaxedPace,
                avoidSeafood,
                wantsLandmarkCeremony,
                prefersIndoorBackup,
                buildTags(relaxedPace, avoidSeafood, wantsLandmarkCeremony, prefersIndoorBackup)
        );
        constraints.setAnalysisSource("rule");
        return constraints;
    }

    private List<String> buildTags(PlanningConstraints constraints) {
        return buildTags(
                constraints.isRelaxedPace(),
                constraints.isAvoidSeafood(),
                constraints.isWantsLandmarkCeremony(),
                constraints.isPrefersIndoorBackup()
        );
    }

    private List<String> buildTags(boolean relaxedPace,
                                   boolean avoidSeafood,
                                   boolean wantsLandmarkCeremony,
                                   boolean prefersIndoorBackup) {
        List<String> extractedTags = new ArrayList<>();
        if (relaxedPace) {
            extractedTags.add("轻松节奏");
        }
        if (avoidSeafood) {
            extractedTags.add("餐饮避开海鲜");
        }
        if (wantsLandmarkCeremony) {
            extractedTags.add("优先考虑仪式感景点");
        }
        if (prefersIndoorBackup) {
            extractedTags.add("预留室内备选方案");
        }
        return extractedTags;
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
