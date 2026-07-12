package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.model.LoopCritique;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.service.RequestOptimizationStrategyService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LoopDecisionExecutor {

    private final RequestOptimizationStrategyService requestOptimizationStrategyService;

    public LoopDecisionExecutor(RequestOptimizationStrategyService requestOptimizationStrategyService) {
        this.requestOptimizationStrategyService = requestOptimizationStrategyService;
    }

    public ExecutionResult apply(TripPlanRequest request, TripPlan plan, LoopCritique critique) {
        TripPlanRequest updatedRequest = requestOptimizationStrategyService.copyRequest(request);
        List<String> changeNotes = new ArrayList<>();
        requestOptimizationStrategyService.applyAction(updatedRequest, plan, critique.getAction(), changeNotes);
        return new ExecutionResult(updatedRequest, changeNotes);
    }

    public static class ExecutionResult {

        private final TripPlanRequest updatedRequest;
        private final List<String> changeNotes;

        public ExecutionResult(TripPlanRequest updatedRequest, List<String> changeNotes) {
            this.updatedRequest = updatedRequest;
            this.changeNotes = changeNotes;
        }

        public TripPlanRequest getUpdatedRequest() {
            return updatedRequest;
        }

        public List<String> getChangeNotes() {
            return changeNotes;
        }
    }
}
