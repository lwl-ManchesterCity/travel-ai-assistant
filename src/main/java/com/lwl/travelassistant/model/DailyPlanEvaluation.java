package com.lwl.travelassistant.model;

public class DailyPlanEvaluation {

    private boolean overBudget;
    private boolean routeTooLong;
    private boolean indoorAdjustmentRecommended;
    private int budgetTolerance;
    private int routeLimitMinutes;
    private int budgetOverflowAmount;
    private int routeOverflowMinutes;
    private double routeOverflowDistanceKm;
    private String budgetDiagnosis;
    private String routeDiagnosis;
    private String weatherDiagnosis;

    public DailyPlanEvaluation() {
    }

    public DailyPlanEvaluation(boolean overBudget,
                               boolean routeTooLong,
                               boolean indoorAdjustmentRecommended,
                               int budgetTolerance,
                               int routeLimitMinutes,
                               int budgetOverflowAmount,
                               int routeOverflowMinutes,
                               double routeOverflowDistanceKm,
                               String budgetDiagnosis,
                               String routeDiagnosis,
                               String weatherDiagnosis) {
        this.overBudget = overBudget;
        this.routeTooLong = routeTooLong;
        this.indoorAdjustmentRecommended = indoorAdjustmentRecommended;
        this.budgetTolerance = budgetTolerance;
        this.routeLimitMinutes = routeLimitMinutes;
        this.budgetOverflowAmount = budgetOverflowAmount;
        this.routeOverflowMinutes = routeOverflowMinutes;
        this.routeOverflowDistanceKm = routeOverflowDistanceKm;
        this.budgetDiagnosis = budgetDiagnosis;
        this.routeDiagnosis = routeDiagnosis;
        this.weatherDiagnosis = weatherDiagnosis;
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

    public int getBudgetTolerance() {
        return budgetTolerance;
    }

    public void setBudgetTolerance(int budgetTolerance) {
        this.budgetTolerance = budgetTolerance;
    }

    public int getRouteLimitMinutes() {
        return routeLimitMinutes;
    }

    public void setRouteLimitMinutes(int routeLimitMinutes) {
        this.routeLimitMinutes = routeLimitMinutes;
    }

    public int getBudgetOverflowAmount() {
        return budgetOverflowAmount;
    }

    public void setBudgetOverflowAmount(int budgetOverflowAmount) {
        this.budgetOverflowAmount = budgetOverflowAmount;
    }

    public int getRouteOverflowMinutes() {
        return routeOverflowMinutes;
    }

    public void setRouteOverflowMinutes(int routeOverflowMinutes) {
        this.routeOverflowMinutes = routeOverflowMinutes;
    }

    public double getRouteOverflowDistanceKm() {
        return routeOverflowDistanceKm;
    }

    public void setRouteOverflowDistanceKm(double routeOverflowDistanceKm) {
        this.routeOverflowDistanceKm = routeOverflowDistanceKm;
    }

    public String getBudgetDiagnosis() {
        return budgetDiagnosis;
    }

    public void setBudgetDiagnosis(String budgetDiagnosis) {
        this.budgetDiagnosis = budgetDiagnosis;
    }

    public String getRouteDiagnosis() {
        return routeDiagnosis;
    }

    public void setRouteDiagnosis(String routeDiagnosis) {
        this.routeDiagnosis = routeDiagnosis;
    }

    public String getWeatherDiagnosis() {
        return weatherDiagnosis;
    }

    public void setWeatherDiagnosis(String weatherDiagnosis) {
        this.weatherDiagnosis = weatherDiagnosis;
    }

    public boolean isAcceptable() {
        return !overBudget && !routeTooLong;
    }
}
