package com.lwl.travelassistant.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.LlmClient;
import com.lwl.travelassistant.model.DayPlan;
import com.lwl.travelassistant.model.LoopCritique;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanReflection;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LlmCriticAgent {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmCriticAgent(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public LoopCritique critique(TripPlanRequest request, TripPlan plan, TripPlanReflection reflection) {
        if (llmClient.isAvailable()) {
            try {
                return critiqueByLlm(request, plan, reflection);
            } catch (Exception exception) {
                LoopCritique fallback = critiqueByRules(request, plan, reflection);
                fallback.setSource("rule-fallback");
                List<String> notes = fallback.getNotes() == null ? new ArrayList<>() : new ArrayList<>(fallback.getNotes());
                notes.add("LLM 批评阶段失败，已回退到规则批评");
                fallback.setNotes(notes);
                return fallback;
            }
        }
        return critiqueByRules(request, plan, reflection);
    }

    private LoopCritique critiqueByLlm(TripPlanRequest request, TripPlan plan, TripPlanReflection reflection) throws Exception {
        String content = llmClient.chat(buildSystemPrompt(), buildUserPrompt(request, plan, reflection));
        JsonNode node = objectMapper.readTree(content);
        return new LoopCritique(
                normalizeAction(node.path("action").asText("STOP")),
                node.path("reason").asText("LLM 未给出原因"),
                node.path("expectedOutcome").asText("维持当前方案"),
                "llm",
                readStringList(node.path("notes"))
        );
    }

    private String buildSystemPrompt() {
        return """
                你是旅行规划批评智能体。请根据当前旅行方案，判断是否需要再规划一轮。
                你必须只返回 JSON，不要返回 Markdown、解释或代码块。
                JSON 格式：
                {
                  "action": string,
                  "reason": string,
                  "expectedOutcome": string,
                  "notes": string[]
                }
                规则：
                1. action 只能从以下枚举中选一个：
                   REDUCE_BUDGET
                   COMPRESS_ROUTE
                   REPLACE_OUTDOOR_WITH_INDOOR
                   UPGRADE_EXPERIENCE
                   STOP
                2. 如果预算明显超支，优先 REDUCE_BUDGET。
                3. 如果预算剩余很多且体验明显偏保守，可考虑 UPGRADE_EXPERIENCE。
                4. 如果天气恶劣且需要室内备选，可考虑 REPLACE_OUTDOOR_WITH_INDOOR。
                5. 如果总通勤过长，可考虑 COMPRESS_ROUTE。
                6. 如果当前方案已经足够合理，返回 STOP。
                7. 请重点参考 reflection 中的 budgetSeverity、routeSeverity、weatherSeverity、experienceSeverity、overallScore。
                8. notes 用简短中文说明批评依据。
                """;
    }

    private String buildUserPrompt(TripPlanRequest request, TripPlan plan, TripPlanReflection reflection) throws Exception {
        return "用户请求：\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)
                + "\n\n规则评估结果：\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reflection)
                + "\n\n当前方案：\n"
                + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
    }

    private LoopCritique critiqueByRules(TripPlanRequest request, TripPlan plan, TripPlanReflection reflection) {
        int totalDurationMinutes = 0;
        boolean rainyWeather = false;
        if (plan.getDays() != null) {
            for (DayPlan dayPlan : plan.getDays()) {
                if (dayPlan.getRoutePlan() != null) {
                    totalDurationMinutes += dayPlan.getRoutePlan().getTotalDurationMinutes();
                }
            }
        }
        if (plan.getWeatherInfo() != null) {
            for (WeatherInfo weatherInfo : plan.getWeatherInfo()) {
                String dayWeather = weatherInfo.getDayWeather();
                if (dayWeather != null && (dayWeather.contains("雨") || dayWeather.contains("雪") || dayWeather.contains("雷"))) {
                    rainyWeather = true;
                    break;
                }
            }
        }

        boolean overBudget = reflection != null ? reflection.isOverBudget() : plan.getBudget() != null && !plan.getBudget().isWithinBudget();
        boolean experienceTooConservative = reflection != null
                ? reflection.getExperienceSeverity() > 0
                : plan.getBudget() != null
                && plan.getBudget().isWithinBudget()
                && request.getBudget() >= 5000
                && plan.getBudget().getRemainingBudget() > Math.max(1800, request.getBudget() / 3);
        boolean routeTooLong = reflection != null
                ? reflection.isRouteTooLong()
                : plan.getDays() != null && !plan.getDays().isEmpty() && totalDurationMinutes > plan.getDays().size() * 180;
        if (reflection != null && reflection.isIndoorAdjustmentRecommended()) {
            rainyWeather = true;
        }
        int budgetSeverity = reflection == null ? 0 : reflection.getBudgetSeverity();
        int routeSeverity = reflection == null ? 0 : reflection.getRouteSeverity();
        int weatherSeverity = reflection == null ? 0 : reflection.getWeatherSeverity();
        int experienceSeverity = reflection == null ? 0 : reflection.getExperienceSeverity();
        int overallScore = reflection == null ? 0 : reflection.getOverallScore();

        List<String> notes = new ArrayList<>();
        if (overBudget) {
            notes.add("当前总预算超支 " + Math.abs(plan.getBudget().getRemainingBudget()) + " 元");
        }
        if (experienceTooConservative) {
            notes.add("预算结余较多，当前方案体验偏保守");
        }
        if (routeTooLong) {
            notes.add("总通勤时长达到 " + totalDurationMinutes + " 分钟");
        }
        if (rainyWeather) {
            notes.add("出行期存在降雨天气，可准备室内备选");
        }
        if (reflection != null) {
            notes.add("评分摘要：overall=" + overallScore
                    + "，budget=" + budgetSeverity
                    + "，route=" + routeSeverity
                    + "，weather=" + weatherSeverity
                    + "，experience=" + experienceSeverity);
        }

        String action;
        String reason;
        String expectedOutcome;
        if (reflection != null && reflection.isAcceptable()) {
            action = "STOP";
            reason = "规则评分已处于可接受范围，无需继续重规划";
            expectedOutcome = "保留当前方案作为最终结果";
        } else if (budgetSeverity >= routeSeverity && budgetSeverity >= weatherSeverity && budgetSeverity >= experienceSeverity && budgetSeverity > 0) {
            action = "REDUCE_BUDGET";
            reason = "当前方案预算严重度最高，建议优先压缩成本后重规划";
            expectedOutcome = "通过降低住宿和交通成本，让总费用回到预算范围附近";
        } else if (routeSeverity >= budgetSeverity && routeSeverity >= weatherSeverity && routeSeverity >= experienceSeverity && routeSeverity > 0) {
            action = "COMPRESS_ROUTE";
            reason = "当前方案路线严重度最高，建议优先压缩跨区通勤";
            expectedOutcome = "减少跨区折返，让总通勤时长明显下降";
        } else if (weatherSeverity >= budgetSeverity && weatherSeverity >= routeSeverity && weatherSeverity >= experienceSeverity && weatherSeverity > 0) {
            action = "REPLACE_OUTDOOR_WITH_INDOOR";
            reason = "当前方案天气风险严重度最高，建议增加室内备选";
            expectedOutcome = "降低天气波动对行程体验的影响";
        } else if (experienceTooConservative) {
            action = "UPGRADE_EXPERIENCE";
            reason = "当前方案体验提升严重度最高，建议适度提升住宿或体验强度";
            expectedOutcome = "适度提升住宿和行程体验，减少预算浪费";
        } else {
            action = "STOP";
            reason = "当前方案整体可接受，无需进入下一轮";
            expectedOutcome = "保留当前方案作为最终结果";
        }

        return new LoopCritique(
                action,
                reason,
                expectedOutcome,
                "rule",
                notes
        );
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "STOP";
        }
        return switch (action.trim().toUpperCase()) {
            case "REDUCE_BUDGET", "COMPRESS_ROUTE", "REPLACE_OUTDOOR_WITH_INDOOR", "UPGRADE_EXPERIENCE", "STOP" -> action.trim().toUpperCase();
            default -> "STOP";
        };
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
}
