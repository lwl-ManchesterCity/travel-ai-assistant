package com.lwl.travelassistant.evaluator;

import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

@Component
public class RouteEvaluator {

    public int determineRouteLimitMinutes(PlanningConstraints constraints, boolean indoorAdjustmentRecommended) {
        int routeLimit = 210;
        if (constraints != null && constraints.isRelaxedPace()) {
            routeLimit = 150;
        }
        if (indoorAdjustmentRecommended) {
            routeLimit = Math.min(routeLimit, 135);
        }
        return routeLimit;
    }

    public boolean isRouteTooLong(RoutePlan routePlan,
                                  PlanningConstraints constraints,
                                  WeatherInfo currentWeather,
                                  boolean indoorAdjustmentRecommended) {
        if (routePlan == null) {
            return false;
        }
        int routeLimit = determineRouteLimitMinutes(constraints, indoorAdjustmentRecommended);
        return routePlan.getTotalDurationMinutes() > routeLimit || routePlan.getTotalDistanceKm() > 70;
    }

    public int calculateRouteOverflowMinutes(RoutePlan routePlan,
                                             PlanningConstraints constraints,
                                             boolean indoorAdjustmentRecommended) {
        if (routePlan == null) {
            return 0;
        }
        int routeLimit = determineRouteLimitMinutes(constraints, indoorAdjustmentRecommended);
        return Math.max(0, routePlan.getTotalDurationMinutes() - routeLimit);
    }

    public double calculateRouteOverflowDistanceKm(RoutePlan routePlan) {
        if (routePlan == null) {
            return 0;
        }
        return Math.max(0, routePlan.getTotalDistanceKm() - 70);
    }
}
