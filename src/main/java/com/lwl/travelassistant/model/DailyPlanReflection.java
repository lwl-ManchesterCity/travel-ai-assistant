package com.lwl.travelassistant.model;

import java.util.List;

public class DailyPlanReflection {

    private DailyPlanEvaluation evaluation;
    private String recommendedAction;
    private String reason;
    private List<String> reflectionNotes;

    public DailyPlanReflection() {
    }

    public DailyPlanReflection(DailyPlanEvaluation evaluation,
                               String recommendedAction,
                               String reason,
                               List<String> reflectionNotes) {
        this.evaluation = evaluation;
        this.recommendedAction = recommendedAction;
        this.reason = reason;
        this.reflectionNotes = reflectionNotes;
    }

    public DailyPlanEvaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(DailyPlanEvaluation evaluation) {
        this.evaluation = evaluation;
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

    public boolean isAcceptable() {
        return evaluation != null && evaluation.isAcceptable();
    }
}
