package com.lwl.travelassistant.evaluator;

import com.lwl.travelassistant.model.DailyPlanEvaluation;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

@Component
public class DailyPlanEvaluator {

    private final BudgetEvaluator budgetEvaluator;
    private final RouteEvaluator routeEvaluator;
    private final WeatherEvaluator weatherEvaluator;

    public DailyPlanEvaluator(BudgetEvaluator budgetEvaluator,
                              RouteEvaluator routeEvaluator,
                              WeatherEvaluator weatherEvaluator) {
        this.budgetEvaluator = budgetEvaluator;
        this.routeEvaluator = routeEvaluator;
        this.weatherEvaluator = weatherEvaluator;
    }

    public DailyPlanEvaluation evaluate(int estimatedCost,
                                        int dailyBudgetLimit,
                                        RoutePlan routePlan,
                                        PlanningConstraints constraints,
                                        WeatherInfo currentWeather) {
        boolean indoorAdjustmentRecommended = weatherEvaluator.shouldPreferIndoor(constraints, currentWeather);
        boolean overBudget = budgetEvaluator.isOverBudget(estimatedCost, dailyBudgetLimit);
        boolean routeTooLong = routeEvaluator.isRouteTooLong(routePlan, constraints, currentWeather, indoorAdjustmentRecommended);
        int routeLimitMinutes = routeEvaluator.determineRouteLimitMinutes(constraints, indoorAdjustmentRecommended);
        int budgetOverflowAmount = Math.max(0, estimatedCost - (dailyBudgetLimit + budgetEvaluator.getBudgetTolerance()));
        int routeOverflowMinutes = routeEvaluator.calculateRouteOverflowMinutes(routePlan, constraints, indoorAdjustmentRecommended);
        double routeOverflowDistanceKm = routeEvaluator.calculateRouteOverflowDistanceKm(routePlan);
        String budgetDiagnosis = overBudget
                ? "当日预计花费超出预算缓冲 " + budgetOverflowAmount + " 元"
                : "当日预算处于可控范围";
        String routeDiagnosis = routeTooLong
                ? "当日通勤超出建议阈值，超出约 " + routeOverflowMinutes + " 分钟"
                : "当日通勤处于可接受范围";
        String weatherDiagnosis = indoorAdjustmentRecommended
                ? "当天天气不佳，建议优先安排室内或低暴露景点"
                : "当天天气对当前景点结构影响较小";
        return new DailyPlanEvaluation(
                overBudget,
                routeTooLong,
                indoorAdjustmentRecommended,
                budgetEvaluator.getBudgetTolerance(),
                routeLimitMinutes,
                budgetOverflowAmount,
                routeOverflowMinutes,
                routeOverflowDistanceKm,
                budgetDiagnosis,
                routeDiagnosis,
                weatherDiagnosis
        );
    }
}
