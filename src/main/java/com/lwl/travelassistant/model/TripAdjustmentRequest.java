package com.lwl.travelassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TripAdjustmentRequest {

    @NotNull
    private TripPlanRequest originalRequest;

    @NotBlank
    private String adjustmentPrompt;

    public TripAdjustmentRequest() {
    }

    public TripAdjustmentRequest(TripPlanRequest originalRequest, String adjustmentPrompt) {
        this.originalRequest = originalRequest;
        this.adjustmentPrompt = adjustmentPrompt;
    }

    public TripPlanRequest getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(TripPlanRequest originalRequest) {
        this.originalRequest = originalRequest;
    }

    public String getAdjustmentPrompt() {
        return adjustmentPrompt;
    }

    public void setAdjustmentPrompt(String adjustmentPrompt) {
        this.adjustmentPrompt = adjustmentPrompt;
    }
}
