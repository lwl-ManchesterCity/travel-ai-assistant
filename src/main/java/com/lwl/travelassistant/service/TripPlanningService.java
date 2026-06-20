package com.lwl.travelassistant.service;

import com.lwl.travelassistant.agent.TripAdjustmentAgent;
import com.lwl.travelassistant.agent.PlannerAgent;
import com.lwl.travelassistant.model.PlannerInput;
import com.lwl.travelassistant.model.TripAdjustmentRequest;
import com.lwl.travelassistant.model.TripAdjustmentResponse;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripPlanningService {

    private final PlannerInputBuilder plannerInputBuilder;
    private final PlannerAgent plannerAgent;
    private final TripAdjustmentAgent tripAdjustmentAgent;

    public TripPlanningService(PlannerInputBuilder plannerInputBuilder,
                               PlannerAgent plannerAgent,
                               TripAdjustmentAgent tripAdjustmentAgent) {
        this.plannerInputBuilder = plannerInputBuilder;
        this.plannerAgent = plannerAgent;
        this.tripAdjustmentAgent = tripAdjustmentAgent;
    }

    public TripPlan planTrip(TripPlanRequest request) {
        PlannerInput plannerInput = plannerInputBuilder.build(request);
        return plannerAgent.buildPlan(plannerInput);
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
