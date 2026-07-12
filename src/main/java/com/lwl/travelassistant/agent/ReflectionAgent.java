package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.evaluator.DailyPlanEvaluator;
import com.lwl.travelassistant.model.DailyPlanEvaluation;
import com.lwl.travelassistant.model.DailyPlanReflection;
import com.lwl.travelassistant.model.DayPlan;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanReflection;
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

    public TripPlanReflection reflectTripPlan(TripPlan tripPlan, TripPlanRequestView requestView) {
        int totalRouteMinutes = 0;
        int routeOverflowMinutes = 0;
        boolean indoorAdjustmentRecommended = false;
        List<String> notes = new ArrayList<>();

        if (tripPlan.getDays() != null) {
            for (DayPlan dayPlan : tripPlan.getDays()) {
                if (dayPlan.getRoutePlan() != null) {
                    totalRouteMinutes += dayPlan.getRoutePlan().getTotalDurationMinutes();
                }
            }
        }
        int expectedRouteLimit = tripPlan.getDays() == null ? 0 : tripPlan.getDays().size() * 180;
        if (totalRouteMinutes > expectedRouteLimit) {
            routeOverflowMinutes = totalRouteMinutes - expectedRouteLimit;
        }
        if (tripPlan.getWeatherInfo() != null) {
            for (WeatherInfo weatherInfo : tripPlan.getWeatherInfo()) {
                String weather = weatherInfo.getDayWeather();
                if (weather != null && (weather.contains("雨") || weather.contains("雪") || weather.contains("雷"))) {
                    indoorAdjustmentRecommended = true;
                    break;
                }
            }
        }

        boolean overBudget = tripPlan.getBudget() != null && !tripPlan.getBudget().isWithinBudget();
        int budgetOverflowAmount = overBudget ? Math.abs(tripPlan.getBudget().getRemainingBudget()) : 0;
        boolean routeTooLong = routeOverflowMinutes > 0;
        int budgetSeverity = calculateBudgetSeverity(overBudget, budgetOverflowAmount, requestView);
        int routeSeverity = calculateRouteSeverity(routeOverflowMinutes, tripPlan);
        int weatherSeverity = calculateWeatherSeverity(indoorAdjustmentRecommended, tripPlan);
        int experienceSeverity = calculateExperienceSeverity(tripPlan, requestView);
        int overallScore = calculateOverallScore(budgetSeverity, routeSeverity, weatherSeverity, experienceSeverity);
        boolean acceptable = overallScore <= 15;
        String action = recommendTripAction(budgetSeverity, routeSeverity, weatherSeverity, experienceSeverity, acceptable);
        String reason = buildTripReason(action, budgetOverflowAmount, routeOverflowMinutes, indoorAdjustmentRecommended, overallScore);

        if (overBudget) {
            notes.add("总预算超支 " + budgetOverflowAmount + " 元");
        }
        if (routeTooLong) {
            notes.add("总通勤超出建议阈值约 " + routeOverflowMinutes + " 分钟");
        }
        if (indoorAdjustmentRecommended) {
            notes.add("出行期存在雨雪雷天气，建议增加室内备选");
        }
        if (experienceSeverity > 0) {
            notes.add("预算结余较多，当前方案体验利用偏保守");
        }
        notes.add("当前评分=" + overallScore
                + "（预算=" + budgetSeverity
                + "，路线=" + routeSeverity
                + "，天气=" + weatherSeverity
                + "，体验=" + experienceSeverity + "）");
        if (acceptable) {
            notes.add("整份方案在预算、路线、天气上都处于可接受范围");
        }

        return new TripPlanReflection(
                acceptable,
                overBudget,
                routeTooLong,
                indoorAdjustmentRecommended,
                budgetOverflowAmount,
                routeOverflowMinutes,
                budgetSeverity,
                routeSeverity,
                weatherSeverity,
                experienceSeverity,
                overallScore,
                action,
                reason,
                notes
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

    private String recommendTripAction(int budgetSeverity,
                                       int routeSeverity,
                                       int weatherSeverity,
                                       int experienceSeverity,
                                       boolean acceptable) {
        if (acceptable) {
            return "STOP";
        }
        if (budgetSeverity >= routeSeverity && budgetSeverity >= weatherSeverity && budgetSeverity >= experienceSeverity && budgetSeverity > 0) {
            return "REDUCE_BUDGET";
        }
        if (routeSeverity >= budgetSeverity && routeSeverity >= weatherSeverity && routeSeverity >= experienceSeverity && routeSeverity > 0) {
            return "COMPRESS_ROUTE";
        }
        if (weatherSeverity >= budgetSeverity && weatherSeverity >= routeSeverity && weatherSeverity >= experienceSeverity && weatherSeverity > 0) {
            return "REPLACE_OUTDOOR_WITH_INDOOR";
        }
        if (experienceSeverity > 0) {
            return "UPGRADE_EXPERIENCE";
        }
        return "STOP";
    }

    private String buildTripReason(String action,
                                   int budgetOverflowAmount,
                                   int routeOverflowMinutes,
                                   boolean indoorAdjustmentRecommended,
                                   int overallScore) {
        return switch (action) {
            case "REDUCE_BUDGET" -> "当前方案整体评分为 " + overallScore + "，其中预算问题最突出，超支 " + budgetOverflowAmount + " 元，建议优先压缩预算";
            case "COMPRESS_ROUTE" -> "当前方案整体评分为 " + overallScore + "，其中路线问题最突出，通勤超出建议阈值 " + routeOverflowMinutes + " 分钟，建议优先压缩路线";
            case "REPLACE_OUTDOOR_WITH_INDOOR" -> indoorAdjustmentRecommended
                    ? "当前方案整体评分为 " + overallScore + "，天气存在风险，建议增加室内备选方案"
                    : "建议替换部分户外景点";
            case "UPGRADE_EXPERIENCE" -> "当前方案整体评分为 " + overallScore + "，预算利用偏保守，可适度提升住宿或体验强度";
            default -> "当前方案整体评分为 " + overallScore + "，已处于可接受范围，无需继续重规划";
        };
    }

    private int calculateBudgetSeverity(boolean overBudget,
                                        int budgetOverflowAmount,
                                        TripPlanRequestView requestView) {
        if (!overBudget || requestView == null || requestView.requestedBudget() <= 0) {
            return 0;
        }
        double ratio = (double) budgetOverflowAmount / requestView.requestedBudget();
        return clampScore((int) Math.round(20 + ratio * 100));
    }

    private int calculateRouteSeverity(int routeOverflowMinutes, TripPlan tripPlan) {
        if (routeOverflowMinutes <= 0 || tripPlan.getDays() == null || tripPlan.getDays().isEmpty()) {
            return 0;
        }
        int expectedRouteLimit = tripPlan.getDays().size() * 180;
        double ratio = expectedRouteLimit <= 0 ? 0 : (double) routeOverflowMinutes / expectedRouteLimit;
        return clampScore((int) Math.round(15 + ratio * 120));
    }

    private int calculateWeatherSeverity(boolean indoorAdjustmentRecommended, TripPlan tripPlan) {
        if (!indoorAdjustmentRecommended || tripPlan.getWeatherInfo() == null || tripPlan.getWeatherInfo().isEmpty()) {
            return 0;
        }
        int riskyDays = 0;
        for (WeatherInfo weatherInfo : tripPlan.getWeatherInfo()) {
            String weather = weatherInfo.getDayWeather();
            if (weather != null && (weather.contains("雨") || weather.contains("雪") || weather.contains("雷"))) {
                riskyDays++;
            }
        }
        return clampScore(riskyDays * 25);
    }

    private int calculateExperienceSeverity(TripPlan tripPlan, TripPlanRequestView requestView) {
        if (tripPlan.getBudget() == null
                || !tripPlan.getBudget().isWithinBudget()
                || requestView == null
                || requestView.requestedBudget() < 5000) {
            return 0;
        }
        int remainingBudget = tripPlan.getBudget().getRemainingBudget();
        int threshold = Math.max(1800, requestView.requestedBudget() / 3);
        if (remainingBudget <= threshold) {
            return 0;
        }
        double ratio = (double) (remainingBudget - threshold) / requestView.requestedBudget();
        return clampScore((int) Math.round(10 + ratio * 80));
    }

    private int calculateOverallScore(int budgetSeverity,
                                      int routeSeverity,
                                      int weatherSeverity,
                                      int experienceSeverity) {
        return budgetSeverity + routeSeverity + weatherSeverity + experienceSeverity;
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    public record TripPlanRequestView(int requestedBudget) {
    }
}
