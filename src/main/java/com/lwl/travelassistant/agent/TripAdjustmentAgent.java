package com.lwl.travelassistant.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.LlmClient;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.service.RequestOptimizationStrategyService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TripAdjustmentAgent {

    private static final Pattern BUDGET_PATTERN = Pattern.compile("(\\d{3,6})");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final RequestOptimizationStrategyService requestOptimizationStrategyService;

    public TripAdjustmentAgent(LlmClient llmClient,
                               ObjectMapper objectMapper,
                               RequestOptimizationStrategyService requestOptimizationStrategyService) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.requestOptimizationStrategyService = requestOptimizationStrategyService;
    }

    public TripAdjustmentDecision adjust(TripPlanRequest originalRequest, String adjustmentPrompt) {
        if (llmClient.isAvailable()) {
            try {
                return adjustByLlm(originalRequest, adjustmentPrompt);
            } catch (Exception exception) {
                TripAdjustmentDecision fallbackDecision = adjustByRules(originalRequest, adjustmentPrompt);
                fallbackDecision.getAppliedChanges().add("LLM 解析失败，已自动回退到规则解析");
                fallbackDecision.setAdjustmentSource("rule-fallback");
                return fallbackDecision;
            }
        }
        return adjustByRules(originalRequest, adjustmentPrompt);
    }

    private TripAdjustmentDecision adjustByLlm(TripPlanRequest originalRequest, String adjustmentPrompt) throws Exception {
        TripPlanRequest updatedRequest = requestOptimizationStrategyService.copyRequest(originalRequest);
        JsonNode adjustmentNode = requestLlmAdjustment(originalRequest, adjustmentPrompt);
        List<String> appliedChanges = new ArrayList<>();

        if (adjustmentNode.hasNonNull("budget")) {
            int budget = adjustmentNode.get("budget").asInt();
            if (budget > 0 && budget != updatedRequest.getBudget()) {
                updatedRequest.setBudget(budget);
                appliedChanges.add("LLM 已将预算调整为 " + budget + " 元");
            }
        }
        if (adjustmentNode.has("transportation")) {
            String transportation = readNullableText(adjustmentNode.get("transportation"));
            if ("智能推荐".equals(transportation)) {
                updatedRequest.setTransportation(null);
                appliedChanges.add("LLM 已改为交通方式智能推荐");
            } else if (transportation != null && !transportation.isBlank()) {
                updatedRequest.setTransportation(transportation);
                appliedChanges.add("LLM 已调整交通方式为 " + transportation);
            }
        }
        if (adjustmentNode.has("accommodation")) {
            String accommodation = readNullableText(adjustmentNode.get("accommodation"));
            if (accommodation != null && !accommodation.isBlank()) {
                updatedRequest.setAccommodation(accommodation);
                appliedChanges.add("LLM 已调整住宿偏好为 " + accommodation);
            }
        }

        applyPreferenceChanges(updatedRequest, adjustmentNode, appliedChanges);
        applyExtraRequirementChanges(updatedRequest, adjustmentPrompt, adjustmentNode, appliedChanges);
        applySharedStrategyActions(updatedRequest, adjustmentPrompt, appliedChanges);

        if (appliedChanges.isEmpty()) {
            appliedChanges.add("LLM 已保留原始规划条件，并记录新的调整要求");
        }
        return new TripAdjustmentDecision(updatedRequest, appliedChanges, "llm");
    }

    private void applyPreferenceChanges(TripPlanRequest updatedRequest,
                                        JsonNode adjustmentNode,
                                        List<String> appliedChanges) {
        List<String> preferences = updatedRequest.getPreferences();
        if (preferences == null) {
            preferences = new ArrayList<>();
            updatedRequest.setPreferences(preferences);
        }

        JsonNode addNode = adjustmentNode.get("preferencesToAdd");
        if (addNode != null && addNode.isArray()) {
            for (JsonNode item : addNode) {
                String preference = item.asText("");
                if (!preference.isBlank() && !preferences.contains(preference)) {
                    preferences.add(preference);
                    appliedChanges.add("LLM 已增加偏好：" + preference);
                }
            }
        }

        JsonNode removeNode = adjustmentNode.get("preferencesToRemove");
        if (removeNode != null && removeNode.isArray()) {
            for (JsonNode item : removeNode) {
                String preference = item.asText("");
                if (!preference.isBlank() && preferences.remove(preference)) {
                    appliedChanges.add("LLM 已移除偏好：" + preference);
                }
            }
        }
    }

    private void applyExtraRequirementChanges(TripPlanRequest updatedRequest,
                                              String adjustmentPrompt,
                                              JsonNode adjustmentNode,
                                              List<String> appliedChanges) {
        List<String> extraRequirements = new ArrayList<>();
        JsonNode extraNode = adjustmentNode.get("extraRequirements");
        if (extraNode != null && extraNode.isArray()) {
            for (JsonNode item : extraNode) {
                String value = item.asText("");
                if (!value.isBlank() && !isAdjustmentInstruction(value)) {
                    extraRequirements.add(value);
                }
            }
        }
        if (extraRequirements.isEmpty() && adjustmentPrompt != null && !adjustmentPrompt.isBlank()) {
            extraRequirements.addAll(extractResidualRequirements(adjustmentPrompt));
        }
        boolean merged = false;
        for (String extraRequirement : extraRequirements) {
            requestOptimizationStrategyService.mergeExtraRequirement(updatedRequest, extraRequirement);
            merged = true;
        }
        if (merged) {
            appliedChanges.add("LLM 已记录新的自然语言调整要求");
        }
    }

    private JsonNode requestLlmAdjustment(TripPlanRequest originalRequest, String adjustmentPrompt) throws Exception {
        String content = llmClient.chat(buildSystemPrompt(), buildUserPrompt(originalRequest, adjustmentPrompt));
        return objectMapper.readTree(content);
    }

    private String buildSystemPrompt() {
        return """
                你是旅行计划修改助手。你的任务不是重新写一份文案，而是把用户的自然语言修改要求解析成结构化 JSON。
                你必须只返回 JSON，不要返回任何解释、Markdown、代码块。
                JSON 字段固定为：
                {
                  "budget": number|null,
                  "transportation": string|null,
                  "accommodation": string|null,
                  "preferencesToAdd": string[],
                  "preferencesToRemove": string[],
                  "extraRequirements": string[]
                }
                规则：
                1. transportation 只允许返回：自驾、公共交通、打车为主、智能推荐、null
                2. accommodation 只允许返回：经济型酒店、便捷酒店、舒适型酒店、高端酒店、民宿、客栈、null
                3. preferences 只允许：人文历史、自然风景、美食、城市漫游、轻松节奏
                4. 如果用户没有明确修改某项，就返回 null 或空数组
                5. extraRequirements 用于保留补充限制，比如“不吃海鲜”“别太赶”“靠近地铁”
                """;
    }

    private String buildUserPrompt(TripPlanRequest originalRequest, String adjustmentPrompt) {
        return "原始请求：\n"
                + objectToPrettyJson(originalRequest)
                + "\n\n用户新的修改要求：\n"
                + adjustmentPrompt;
    }

    private String objectToPrettyJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception exception) {
            return String.valueOf(object);
        }
    }

    private String readNullableText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText("");
        return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }

    private TripAdjustmentDecision adjustByRules(TripPlanRequest originalRequest, String adjustmentPrompt) {
        TripPlanRequest updatedRequest = requestOptimizationStrategyService.copyRequest(originalRequest);
        List<String> appliedChanges = new ArrayList<>();
        String normalizedPrompt = adjustmentPrompt == null ? "" : adjustmentPrompt.trim();
        String lowerPrompt = normalizedPrompt.toLowerCase(Locale.ROOT);

        applyBudgetAdjustment(updatedRequest, normalizedPrompt, lowerPrompt, appliedChanges);
        applyTransportationAdjustment(updatedRequest, normalizedPrompt, lowerPrompt, appliedChanges);
        applyAccommodationAdjustment(updatedRequest, normalizedPrompt, lowerPrompt, appliedChanges);
        applyPreferenceAdjustment(updatedRequest, normalizedPrompt, lowerPrompt, appliedChanges);
        applyExtraRequirementAdjustment(updatedRequest, normalizedPrompt, appliedChanges);
        applySharedStrategyActions(updatedRequest, normalizedPrompt, appliedChanges);

        if (appliedChanges.isEmpty()) {
            appliedChanges.add("已保留原始规划条件，并记录新的调整要求");
        }
        return new TripAdjustmentDecision(updatedRequest, appliedChanges, "rule");
    }

    private void applyBudgetAdjustment(TripPlanRequest updatedRequest,
                                       String normalizedPrompt,
                                       String lowerPrompt,
                                       List<String> appliedChanges) {
        if (!normalizedPrompt.contains("预算")) {
            return;
        }
        Matcher matcher = BUDGET_PATTERN.matcher(normalizedPrompt);
        if (matcher.find()) {
            int budget = Integer.parseInt(matcher.group(1));
            if (budget > 0 && budget != updatedRequest.getBudget()) {
                updatedRequest.setBudget(budget);
                appliedChanges.add("已将预算调整为 " + budget + " 元");
            }
        }
        if (containsAny(lowerPrompt, "省一点", "便宜点", "控制预算")) {
            requestOptimizationStrategyService.mergeExtraRequirement(updatedRequest, "控制预算，优先高性价比方案");
            appliedChanges.add("已追加预算收紧约束");
        }
    }

    private void applyTransportationAdjustment(TripPlanRequest updatedRequest,
                                               String normalizedPrompt,
                                               String lowerPrompt,
                                               List<String> appliedChanges) {
        if (normalizedPrompt.contains("自驾")) {
            updatedRequest.setTransportation("自驾");
            appliedChanges.add("已切换为自驾方案");
            return;
        }
        if (containsAny(normalizedPrompt, "打车", "滴滴")) {
            updatedRequest.setTransportation("打车为主");
            appliedChanges.add("已切换为打车为主");
            return;
        }
        if (containsAny(normalizedPrompt, "公共交通", "地铁", "公交")) {
            updatedRequest.setTransportation("公共交通");
            appliedChanges.add("已切换为公共交通");
            return;
        }
        if (containsAny(lowerPrompt, "智能推荐", "你来安排交通")) {
            updatedRequest.setTransportation(null);
            appliedChanges.add("已改为交通方式智能推荐");
        }
    }

    private void applyAccommodationAdjustment(TripPlanRequest updatedRequest,
                                              String normalizedPrompt,
                                              String lowerPrompt,
                                              List<String> appliedChanges) {
        if (containsAny(normalizedPrompt, "经济", "便宜酒店", "省钱酒店")) {
            updatedRequest.setAccommodation("经济型酒店");
            appliedChanges.add("已切换为经济型酒店偏好");
            return;
        }
        if (containsAny(normalizedPrompt, "便捷酒店", "快捷酒店")) {
            updatedRequest.setAccommodation("便捷酒店");
            appliedChanges.add("已切换为便捷酒店偏好");
            return;
        }
        if (containsAny(normalizedPrompt, "舒适", "舒服一点")) {
            updatedRequest.setAccommodation("舒适型酒店");
            appliedChanges.add("已切换为舒适型酒店偏好");
            return;
        }
        if (containsAny(normalizedPrompt, "高端", "品质酒店", "豪华")) {
            updatedRequest.setAccommodation("高端酒店");
            appliedChanges.add("已切换为高端酒店偏好");
            return;
        }
        if (containsAny(normalizedPrompt, "民宿")) {
            updatedRequest.setAccommodation("民宿");
            appliedChanges.add("已切换为民宿偏好");
            return;
        }
        if (containsAny(lowerPrompt, "客栈")) {
            updatedRequest.setAccommodation("客栈");
            appliedChanges.add("已切换为客栈偏好");
        }
    }

    private void applyPreferenceAdjustment(TripPlanRequest updatedRequest,
                                           String normalizedPrompt,
                                           String lowerPrompt,
                                           List<String> appliedChanges) {
        updatePreference(updatedRequest, normalizedPrompt, lowerPrompt, "人文历史", List.of("历史", "博物馆", "人文"), appliedChanges);
        updatePreference(updatedRequest, normalizedPrompt, lowerPrompt, "自然风景", List.of("自然", "公园", "风景", "户外"), appliedChanges);
        updatePreference(updatedRequest, normalizedPrompt, lowerPrompt, "美食", List.of("美食", "小吃", "吃"), appliedChanges);
        updatePreference(updatedRequest, normalizedPrompt, lowerPrompt, "城市漫游", List.of("艺术", "城市漫游", "展览", "citywalk"), appliedChanges);
        updatePreference(updatedRequest, normalizedPrompt, lowerPrompt, "轻松节奏", List.of("轻松", "慢一点", "别太赶", "悠闲"), appliedChanges);
    }

    private void updatePreference(TripPlanRequest updatedRequest,
                                  String normalizedPrompt,
                                  String lowerPrompt,
                                  String preference,
                                  List<String> keywords,
                                  List<String> appliedChanges) {
        List<String> preferences = updatedRequest.getPreferences();
        if (preferences == null) {
            preferences = new ArrayList<>();
            updatedRequest.setPreferences(preferences);
        }
        boolean wantsRemove = containsAny(normalizedPrompt, "不要" + preference, "不想要" + preference)
                || keywords.stream().anyMatch(keyword -> normalizedPrompt.contains("不要" + keyword) || normalizedPrompt.contains("不想去" + keyword));
        if (wantsRemove) {
            if (preferences.remove(preference)) {
                appliedChanges.add("已移除偏好：" + preference);
            }
            return;
        }
        boolean wantsAdd = keywords.stream().anyMatch(normalizedPrompt::contains)
                || ("轻松节奏".equals(preference) && containsAny(lowerPrompt, "轻松", "慢一点", "悠闲"));
        if (wantsAdd && !preferences.contains(preference)) {
            preferences.add(preference);
            appliedChanges.add("已增加偏好：" + preference);
        }
    }

    private void applyExtraRequirementAdjustment(TripPlanRequest updatedRequest,
                                                 String normalizedPrompt,
                                                 List<String> appliedChanges) {
        if (normalizedPrompt.isBlank()) {
            return;
        }
        List<String> residualRequirements = extractResidualRequirements(normalizedPrompt);
        boolean merged = false;
        for (String residualRequirement : residualRequirements) {
            requestOptimizationStrategyService.mergeExtraRequirement(updatedRequest, residualRequirement);
            merged = true;
        }
        if (merged) {
            appliedChanges.add("已记录新的自然语言调整要求");
        }
    }

    private void applySharedStrategyActions(TripPlanRequest updatedRequest,
                                            String prompt,
                                            List<String> appliedChanges) {
        if (prompt == null || prompt.isBlank()) {
            return;
        }
        String normalized = prompt.toLowerCase(Locale.ROOT);
        applySharedAction(updatedRequest, appliedChanges, normalized, "REDUCE_BUDGET", "已应用统一预算收紧策略",
                "省一点", "便宜点", "控制预算", "缩预算", "预算紧");
        applySharedAction(updatedRequest, appliedChanges, normalized, "COMPRESS_ROUTE", "已应用统一压缩路线策略",
                "路线紧凑", "少折返", "别太绕", "压缩路线", "近一点", "同片区");
        applySharedAction(updatedRequest, appliedChanges, normalized, "REPLACE_OUTDOOR_WITH_INDOOR", "已应用统一室内备选策略",
                "下雨", "室内", "雨天", "别晒", "避雨");
        applySharedAction(updatedRequest, appliedChanges, normalized, "UPGRADE_EXPERIENCE", "已应用统一体验升级策略",
                "品质一点", "高端一点", "住好一点", "体验好一点", "升级体验");
    }

    private void applySharedAction(TripPlanRequest updatedRequest,
                                   List<String> appliedChanges,
                                   String normalizedPrompt,
                                   String action,
                                   String appliedMessage,
                                   String... keywords) {
        for (String keyword : keywords) {
            if (normalizedPrompt.contains(keyword.toLowerCase(Locale.ROOT))) {
                List<String> strategyNotes = new ArrayList<>();
                requestOptimizationStrategyService.applyAction(updatedRequest, null, action, strategyNotes);
                if (!strategyNotes.isEmpty()) {
                    appliedChanges.add(appliedMessage);
                    appliedChanges.addAll(strategyNotes);
                }
                return;
            }
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractResidualRequirements(String prompt) {
        List<String> residualRequirements = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) {
            return residualRequirements;
        }
        String[] clauses = prompt.split("[；;\\n]");
        for (String clause : clauses) {
            String trimmed = clause.trim();
            if (trimmed.isBlank() || isAdjustmentInstruction(trimmed)) {
                continue;
            }
            residualRequirements.add(trimmed);
        }
        return residualRequirements;
    }

    private boolean isAdjustmentInstruction(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.contains("预算")
                || normalized.contains("交通")
                || normalized.contains("自驾")
                || normalized.contains("地铁")
                || normalized.contains("公交")
                || normalized.contains("打车")
                || normalized.contains("酒店")
                || normalized.contains("住宿")
                || normalized.contains("民宿")
                || normalized.contains("客栈")
                || normalized.contains("经济型")
                || normalized.contains("高端")
                || normalized.contains("舒适型")
                || normalized.contains("便捷")
                || normalized.contains("自然风景")
                || normalized.contains("人文历史")
                || normalized.contains("城市漫游")
                || normalized.contains("轻松节奏")
                || normalized.contains("美食")
                || normalized.contains("节奏")
                || normalized.contains("慢一点")
                || normalized.contains("多一点")
                || normalized.contains("少一点");
    }

    public static class TripAdjustmentDecision {

        private final TripPlanRequest updatedRequest;
        private final List<String> appliedChanges;
        private String adjustmentSource;

        public TripAdjustmentDecision(TripPlanRequest updatedRequest, List<String> appliedChanges, String adjustmentSource) {
            this.updatedRequest = updatedRequest;
            this.appliedChanges = appliedChanges;
            this.adjustmentSource = adjustmentSource;
        }

        public TripPlanRequest getUpdatedRequest() {
            return updatedRequest;
        }

        public List<String> getAppliedChanges() {
            return appliedChanges;
        }

        public String getAdjustmentSource() {
            return adjustmentSource;
        }

        public void setAdjustmentSource(String adjustmentSource) {
            this.adjustmentSource = adjustmentSource;
        }
    }
}
