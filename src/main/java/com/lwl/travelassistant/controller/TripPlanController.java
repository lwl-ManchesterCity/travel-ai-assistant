package com.lwl.travelassistant.controller;

import com.lwl.travelassistant.model.TripAdjustmentRequest;
import com.lwl.travelassistant.model.TripAdjustmentResponse;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.service.TripPlanningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin(origins = "*")
public class TripPlanController {

    private final TripPlanningService tripPlanningService;

    public TripPlanController(TripPlanningService tripPlanningService) {
        this.tripPlanningService = tripPlanningService;
    }

    @PostMapping("/plan")
    public TripPlan planTrip(@Valid @RequestBody TripPlanRequest request) {
        return tripPlanningService.planTrip(request);
    }

    @PostMapping("/adjust")
    public TripAdjustmentResponse adjustTrip(@Valid @RequestBody TripAdjustmentRequest request) {
        return tripPlanningService.adjustTrip(request);
    }
}
