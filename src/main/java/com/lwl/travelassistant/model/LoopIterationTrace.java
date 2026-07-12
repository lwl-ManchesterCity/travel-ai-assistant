package com.lwl.travelassistant.model;

import java.util.List;

public class LoopIterationTrace {

    private int round;
    private String requestSummary;
    private int totalCost;
    private int remainingBudget;
    private int totalRouteMinutes;
    private int reflectionScore;
    private int budgetSeverity;
    private int routeSeverity;
    private int weatherSeverity;
    private int experienceSeverity;
    private boolean withinBudget;
    private boolean shouldReplan;
    private String reflectionAction;
    private String reflectionReason;
    private List<String> reflectionNotes;
    private String critiqueSource;
    private String critiqueAction;
    private String critiqueReason;
    private List<String> critiqueNotes;
    private List<String> appliedChanges;
    private boolean selectedAsBest;

    public LoopIterationTrace() {
    }

    public LoopIterationTrace(int round,
                              String requestSummary,
                              int totalCost,
                              int remainingBudget,
                              int totalRouteMinutes,
                              int reflectionScore,
                              int budgetSeverity,
                              int routeSeverity,
                              int weatherSeverity,
                              int experienceSeverity,
                              boolean withinBudget,
                              boolean shouldReplan,
                              String reflectionAction,
                              String reflectionReason,
                              List<String> reflectionNotes,
                              String critiqueSource,
                              String critiqueAction,
                              String critiqueReason,
                              List<String> critiqueNotes,
                              List<String> appliedChanges,
                              boolean selectedAsBest) {
        this.round = round;
        this.requestSummary = requestSummary;
        this.totalCost = totalCost;
        this.remainingBudget = remainingBudget;
        this.totalRouteMinutes = totalRouteMinutes;
        this.reflectionScore = reflectionScore;
        this.budgetSeverity = budgetSeverity;
        this.routeSeverity = routeSeverity;
        this.weatherSeverity = weatherSeverity;
        this.experienceSeverity = experienceSeverity;
        this.withinBudget = withinBudget;
        this.shouldReplan = shouldReplan;
        this.reflectionAction = reflectionAction;
        this.reflectionReason = reflectionReason;
        this.reflectionNotes = reflectionNotes;
        this.critiqueSource = critiqueSource;
        this.critiqueAction = critiqueAction;
        this.critiqueReason = critiqueReason;
        this.critiqueNotes = critiqueNotes;
        this.appliedChanges = appliedChanges;
        this.selectedAsBest = selectedAsBest;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public void setRequestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(int totalCost) {
        this.totalCost = totalCost;
    }

    public int getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(int remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public int getTotalRouteMinutes() {
        return totalRouteMinutes;
    }

    public void setTotalRouteMinutes(int totalRouteMinutes) {
        this.totalRouteMinutes = totalRouteMinutes;
    }

    public int getReflectionScore() {
        return reflectionScore;
    }

    public void setReflectionScore(int reflectionScore) {
        this.reflectionScore = reflectionScore;
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

    public boolean isWithinBudget() {
        return withinBudget;
    }

    public void setWithinBudget(boolean withinBudget) {
        this.withinBudget = withinBudget;
    }

    public boolean isShouldReplan() {
        return shouldReplan;
    }

    public void setShouldReplan(boolean shouldReplan) {
        this.shouldReplan = shouldReplan;
    }

    public String getReflectionAction() {
        return reflectionAction;
    }

    public void setReflectionAction(String reflectionAction) {
        this.reflectionAction = reflectionAction;
    }

    public String getReflectionReason() {
        return reflectionReason;
    }

    public void setReflectionReason(String reflectionReason) {
        this.reflectionReason = reflectionReason;
    }

    public List<String> getReflectionNotes() {
        return reflectionNotes;
    }

    public void setReflectionNotes(List<String> reflectionNotes) {
        this.reflectionNotes = reflectionNotes;
    }

    public String getCritiqueSource() {
        return critiqueSource;
    }

    public void setCritiqueSource(String critiqueSource) {
        this.critiqueSource = critiqueSource;
    }

    public String getCritiqueAction() {
        return critiqueAction;
    }

    public void setCritiqueAction(String critiqueAction) {
        this.critiqueAction = critiqueAction;
    }

    public String getCritiqueReason() {
        return critiqueReason;
    }

    public void setCritiqueReason(String critiqueReason) {
        this.critiqueReason = critiqueReason;
    }

    public List<String> getCritiqueNotes() {
        return critiqueNotes;
    }

    public void setCritiqueNotes(List<String> critiqueNotes) {
        this.critiqueNotes = critiqueNotes;
    }

    public List<String> getAppliedChanges() {
        return appliedChanges;
    }

    public void setAppliedChanges(List<String> appliedChanges) {
        this.appliedChanges = appliedChanges;
    }

    public boolean isSelectedAsBest() {
        return selectedAsBest;
    }

    public void setSelectedAsBest(boolean selectedAsBest) {
        this.selectedAsBest = selectedAsBest;
    }
}
