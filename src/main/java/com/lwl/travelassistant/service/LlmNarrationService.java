package com.lwl.travelassistant.service;

import com.lwl.travelassistant.client.LlmClient;
import com.lwl.travelassistant.model.DayPlan;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LlmNarrationService {

    private final LlmClient llmClient;

    public LlmNarrationService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public boolean isAvailable() {
        return llmClient.isAvailable();
    }

    public String polishDayDescription(TripPlanRequest request, DayPlan dayPlan) {
        String fallback = dayPlan.getDescription();
        if (!isAvailable()) {
            return fallback;
        }
        try {
            String prompt = buildDayPrompt(request, dayPlan);
            String result = llmClient.chat("你是一个中文旅游规划文案助手。输出尽量自然、准确、简洁。", prompt);
            return result == null || result.isBlank() ? fallback : result.trim();
        } catch (Exception exception) {
            return fallback;
        }
    }

    public String polishOverallSuggestions(TripPlanRequest request,
                                           List<WeatherInfo> weatherInfo,
                                           List<DayPlan> dayPlans,
                                           List<String> planningNotes,
                                           String fallback) {
        if (!isAvailable()) {
            return fallback;
        }
        try {
            String prompt = buildOverallPrompt(request, weatherInfo, dayPlans, planningNotes, fallback);
            String result = llmClient.chat("你是一个中文旅游规划文案助手。输出尽量自然、准确、简洁。", prompt);
            return result == null || result.isBlank() ? fallback : result.trim();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String buildDayPrompt(TripPlanRequest request, DayPlan dayPlan) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是旅游行程文案助手。请把下面这一天的规则规划结果，改写成自然、简洁、有一点智能体感的中文描述。\n");
        builder.append("要求：\n");
        builder.append("1. 只输出一段中文，不要标题，不要 Markdown。\n");
        builder.append("2. 长度控制在 80 到 140 字。\n");
        builder.append("3. 要保留核心事实：第几天、核心景点、预算、路线/节奏/天气相关提示。\n");
        builder.append("4. 不要编造不存在的信息。\n\n");
        builder.append("用户请求：").append(safe(request.getCity())).append("，预算 ").append(request.getBudget()).append(" 元，偏好 ")
                .append(request.getPreferences()).append("。\n");
        builder.append("日期：").append(safe(dayPlan.getDate())).append("\n");
        builder.append("原始描述：").append(safe(dayPlan.getDescription())).append("\n");
        builder.append("交通方式：").append(safe(dayPlan.getTransportation())).append("\n");
        builder.append("酒店：").append(dayPlan.getHotel() == null ? "无" : safe(dayPlan.getHotel().getName())).append("\n");
        builder.append("景点：").append(joinAttractionNames(dayPlan)).append("\n");
        builder.append("当日预计花费：").append(dayPlan.getEstimatedCost()).append(" 元\n");
        RoutePlan routePlan = dayPlan.getRoutePlan();
        if (routePlan != null) {
            builder.append("路线摘要：").append(safe(routePlan.getSummary())).append("\n");
        }
        if (dayPlan.getOptimizationNotes() != null && !dayPlan.getOptimizationNotes().isEmpty()) {
            builder.append("自动优化说明：").append(dayPlan.getOptimizationNotes()).append("\n");
        }
        return builder.toString();
    }

    private String buildOverallPrompt(TripPlanRequest request,
                                      List<WeatherInfo> weatherInfo,
                                      List<DayPlan> dayPlans,
                                      List<String> planningNotes,
                                      String fallback) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是旅游计划总览文案助手。请把下面的规则规划总结改写成简洁、自然、有智能规划感的中文总览。\n");
        builder.append("要求：\n");
        builder.append("1. 只输出一段中文，不要标题，不要 Markdown。\n");
        builder.append("2. 长度控制在 120 到 200 字。\n");
        builder.append("3. 要点要包含：城市、天数、偏好、首日天气、预算、住宿偏好、整体节奏或路线提示。\n");
        builder.append("4. 不要编造不存在的信息。\n\n");
        builder.append("城市：").append(safe(request.getCity())).append("\n");
        builder.append("日期：").append(safe(request.getStartDate())).append(" 至 ").append(safe(request.getEndDate())).append("\n");
        builder.append("预算：").append(request.getBudget()).append(" 元\n");
        builder.append("住宿偏好：").append(safe(request.getAccommodation())).append("\n");
        builder.append("偏好：").append(request.getPreferences()).append("\n");
        builder.append("首日天气：").append(weatherInfo == null || weatherInfo.isEmpty() ? "未知" : safe(weatherInfo.get(0).getDayWeather())).append("\n");
        builder.append("总天数：").append(dayPlans == null ? 0 : dayPlans.size()).append("\n");
        if (planningNotes != null && !planningNotes.isEmpty()) {
            builder.append("规划备注：").append(planningNotes).append("\n");
        }
        builder.append("原始总结：").append(safe(fallback)).append("\n");
        return builder.toString();
    }

    private String joinAttractionNames(DayPlan dayPlan) {
        if (dayPlan.getAttractions() == null || dayPlan.getAttractions().isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < dayPlan.getAttractions().size(); index++) {
            if (index > 0) {
                builder.append("、");
            }
            builder.append(dayPlan.getAttractions().get(index).getName());
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
