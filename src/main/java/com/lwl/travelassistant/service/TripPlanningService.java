package com.lwl.travelassistant.service;

import com.lwl.travelassistant.agent.LoopPlanningAgent;
import com.lwl.travelassistant.agent.PlannerAgent;
import com.lwl.travelassistant.agent.TripAdjustmentAgent;
import com.lwl.travelassistant.model.TripAdjustmentRequest;
import com.lwl.travelassistant.model.TripAdjustmentResponse;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripPlanningService {

    private final TripAdjustmentAgent tripAdjustmentAgent;
    private final LoopPlanningAgent loopPlanningAgent;

    public TripPlanningService(PlannerInputBuilder plannerInputBuilder,
                               PlannerAgent plannerAgent,
                               TripAdjustmentAgent tripAdjustmentAgent,
                               LoopPlanningAgent loopPlanningAgent) {
        this.tripAdjustmentAgent = tripAdjustmentAgent;
        this.loopPlanningAgent = loopPlanningAgent;
    }

    public TripPlan planTrip(TripPlanRequest request) {
        return loopPlanningAgent.plan(request);
    }

    public TripAdjustmentResponse adjustTrip(TripAdjustmentRequest request) {
        TripAdjustmentAgent.TripAdjustmentDecision decision = tripAdjustmentAgent.adjust(
                request.getOriginalRequest(),
                request.getAdjustmentPrompt()
        );
        TripPlan tripPlan = planTrip(decision.getUpdatedRequest());
        List<String> planningNotes = tripPlan.getPlanningNotes() == null
                ? new ArrayList<>()
                : new ArrayList<>(tripPlan.getPlanningNotes());
        planningNotes.add("改计划解析来源：" + decision.getAdjustmentSource());
        planningNotes.add("本次改计划要求：" + request.getAdjustmentPrompt());
        planningNotes.addAll(decision.getAppliedChanges());
        tripPlan.setPlanningNotes(planningNotes);
        return new TripAdjustmentResponse(tripPlan, decision.getUpdatedRequest(), decision.getAppliedChanges());
    }
}
