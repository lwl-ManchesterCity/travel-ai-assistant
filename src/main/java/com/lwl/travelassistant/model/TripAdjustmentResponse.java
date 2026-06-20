package com.lwl.travelassistant.model;

import java.util.List;

public class TripAdjustmentResponse {

    private TripPlan tripPlan;
    private TripPlanRequest updatedRequest;
    private List<String> appliedChanges;

    public TripAdjustmentResponse() {
    }

    public TripAdjustmentResponse(TripPlan tripPlan,
                                  TripPlanRequest updatedRequest,
                                  List<String> appliedChanges) {
        this.tripPlan = tripPlan;
        this.updatedRequest = updatedRequest;
        this.appliedChanges = appliedChanges;
    }

    public TripPlan getTripPlan() {
        return tripPlan;
    }

    public void setTripPlan(TripPlan tripPlan) {
        this.tripPlan = tripPlan;
    }

    public TripPlanRequest getUpdatedRequest() {
        return updatedRequest;
    }

    public void setUpdatedRequest(TripPlanRequest updatedRequest) {
        this.updatedRequest = updatedRequest;
    }

    public List<String> getAppliedChanges() {
        return appliedChanges;
    }

    public void setAppliedChanges(List<String> appliedChanges) {
        this.appliedChanges = appliedChanges;
    }
}
