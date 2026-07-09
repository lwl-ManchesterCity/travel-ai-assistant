package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.evaluator.DailyPlanEvaluator;
import com.lwl.travelassistant.model.DailyPlanEvaluation;
import com.lwl.travelassistant.model.DailyPlanReflection;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReflectionAgent {

    private final DailyPlanEvaluator dailyPlanEvaluator;

    public ReflectionAgent(DailyPlanEvaluator dailyPlanEvaluator) {
        this.dailyPlanEvaluator = dailyPlanEvaluator;
    }

    public DailyPlanReflection reflectDailyPlan(int estimatedCost,
                                                int dailyBudgetLimit,
                                                RoutePlan routePlan,
                                                PlanningConstraints constraints,
                                                WeatherInfo currentWeather) {
        DailyPlanEvaluation evaluation = dailyPlanEvaluator.evaluate(
                estimatedCost,
                dailyBudgetLimit,
                routePlan,
                constraints,
                currentWeather
        );
        String action = recommendAction(evaluation);
        return new DailyPlanReflection(
                evaluation,
                action,
                buildReason(evaluation, action),
                buildReflectionNotes(evaluation, action)
        );
    }

    private String recommendAction(DailyPlanEvaluation evaluation) {
        if (evaluation.isAcceptable()) {
            return "accept";
        }
        if (evaluation.isIndoorAdjustmentRecommended()) {
            return "replace_with_indoor";
        }
        if (evaluation.isOverBudget()) {
            return "reduce_budget";
        }
        if (evaluation.isRouteTooLong()) {
            return "compress_route";
        }
        return "manual_review";
    }

    private String buildReason(DailyPlanEvaluation evaluation, String action) {
        return switch (action) {
            case "accept" -> "预算和路线均处于可接受范围";
            case "replace_with_indoor" -> evaluation.getWeatherDiagnosis();
            case "reduce_budget" -> evaluation.getBudgetDiagnosis();
            case "compress_route" -> evaluation.getRouteDiagnosis();
            default -> "当前方案存在约束冲突，需要进入保底调整";
        };
    }

    private List<String> buildReflectionNotes(DailyPlanEvaluation evaluation, String action) {
        List<String> notes = new ArrayList<>();
        notes.add("ReflectionAgent建议：" + translateAction(action));
        if (evaluation.isOverBudget()) {
            notes.add(evaluation.getBudgetDiagnosis());
        }
        if (evaluation.isRouteTooLong()) {
            notes.add(evaluation.getRouteDiagnosis());
        }
        if (evaluation.isIndoorAdjustmentRecommended()) {
            notes.add(evaluation.getWeatherDiagnosis());
        }
        return notes;
    }

    private String translateAction(String action) {
        return switch (action) {
            case "accept" -> "接受当前方案";
            case "replace_with_indoor" -> "优先替换为室内或低暴露景点";
            case "reduce_budget" -> "优先压缩预算";
            case "compress_route" -> "优先压缩通勤";
            default -> "进入保底调整";
        };
    }
}
