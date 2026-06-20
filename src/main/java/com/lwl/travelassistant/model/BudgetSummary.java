package com.lwl.travelassistant.model;

public class BudgetSummary {

    private int requestedBudget;
    private int estimatedTotal;
    private int remainingBudget;
    private String level;

    public BudgetSummary() {
    }

    public BudgetSummary(int requestedBudget, int estimatedTotal, int remainingBudget, String level) {
        this.requestedBudget = requestedBudget;
        this.estimatedTotal = estimatedTotal;
        this.remainingBudget = remainingBudget;
        this.level = level;
    }

    public int getRequestedBudget() {
        return requestedBudget;
    }

    public void setRequestedBudget(int requestedBudget) {
        this.requestedBudget = requestedBudget;
    }

    public int getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(int estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public int getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(int remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
