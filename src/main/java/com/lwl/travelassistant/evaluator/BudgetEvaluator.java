package com.lwl.travelassistant.evaluator;

import org.springframework.stereotype.Component;

@Component
public class BudgetEvaluator {

    public int getBudgetTolerance() {
        return 120;
    }

    public boolean isOverBudget(int estimatedCost, int dailyBudgetLimit) {
        return estimatedCost > dailyBudgetLimit + getBudgetTolerance();
    }
}
