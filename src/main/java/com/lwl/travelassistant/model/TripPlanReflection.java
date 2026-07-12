package com.lwl.travelassistant.model;

import java.util.List;

public class TripPlanReflection {

    private boolean acceptable;
    private boolean overBudget;
    private boolean routeTooLong;
    private boolean indoorAdjustmentRecommended;
    private int totalBudgetOverflowAmount;
    private int totalRouteOverflowMinutes;
    private int budgetSeverity;
    private int routeSeverity;
    private int weatherSeverity;
    private int experienceSeverity;
    private int overallScore;
    private String recommendedAction;
    private String reason;
    private List<String> reflectionNotes;

    public TripPlanReflection() {
    }

    public TripPlanReflection(boolean acceptable,
                              boolean overBudget,
                              boolean routeTooLong,
                              boolean indoorAdjustmentRecommended,
                              int totalBudgetOverflowAmount,
                              int totalRouteOverflowMinutes,
                              int budgetSeverity,
                              int routeSeverity,
                              int weatherSeverity,
                              int experienceSeverity,
                              int overallScore,
                              String recommendedAction,
                              String reason,
                              List<String> reflectionNotes) {
        this.acceptable = acceptable;
        this.overBudget = overBudget;
        this.routeTooLong = routeTooLong;
        this.indoorAdjustmentRecommended = indoorAdjustmentRecommended;
        this.totalBudgetOverflowAmount = totalBudgetOverflowAmount;
        this.totalRouteOverflowMinutes = totalRouteOverflowMinutes;
        this.budgetSeverity = budgetSeverity;
        this.routeSeverity = routeSeverity;
        this.weatherSeverity = weatherSeverity;
        this.experienceSeverity = experienceSeverity;
        this.overallScore = overallScore;
        this.recommendedAction = recommendedAction;
        this.reason = reason;
        this.reflectionNotes = reflectionNotes;
    }

    public boolean isAcceptable() {
        return acceptable;
    }

    public void setAcceptable(boolean acceptable) {
        this.acceptable = acceptable;
    }

    public boolean isOverBudget() {
        return overBudget;
    }

    public void setOverBudget(boolean overBudget) {
        this.overBudget = overBudget;
    }

    public boolean isRouteTooLong() {
        return routeTooLong;
    }

    public void setRouteTooLong(boolean routeTooLong) {
        this.routeTooLong = routeTooLong;
    }

    public boolean isIndoorAdjustmentRecommended() {
        return indoorAdjustmentRecommended;
    }

    public void setIndoorAdjustmentRecommended(boolean indoorAdjustmentRecommended) {
        this.indoorAdjustmentRecommended = indoorAdjustmentRecommended;
    }

    public int getTotalBudgetOverflowAmount() {
        return totalBudgetOverflowAmount;
    }

    public void setTotalBudgetOverflowAmount(int totalBudgetOverflowAmount) {
        this.totalBudgetOverflowAmount = totalBudgetOverflowAmount;
    }

    public int getTotalRouteOverflowMinutes() {
        return totalRouteOverflowMinutes;
    }

    public void setTotalRouteOverflowMinutes(int totalRouteOverflowMinutes) {
        this.totalRouteOverflowMinutes = totalRouteOverflowMinutes;
    }

    public int getBudgetSeverity() {
        return budgetSeverity;
    }

    public void setBudgetSeverity(int budgetSeverity) {
        this.budgetSeverity = budgetSeverity;
    }

    public int getRouteSeverity() {
        return routeSeverity;
    }

    public void setRouteSeverity(int routeSeverity) {
        this.routeSeverity = routeSeverity;
    }

    public int getWeatherSeverity() {
        return weatherSeverity;
    }

    public void setWeatherSeverity(int weatherSeverity) {
        this.weatherSeverity = weatherSeverity;
    }

    public int getExperienceSeverity() {
        return experienceSeverity;
    }

    public void setExperienceSeverity(int experienceSeverity) {
        this.experienceSeverity = experienceSeverity;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getReflectionNotes() {
        return reflectionNotes;
    }

    public void setReflectionNotes(List<String> reflectionNotes) {
        this.reflectionNotes = reflectionNotes;
    }
}
