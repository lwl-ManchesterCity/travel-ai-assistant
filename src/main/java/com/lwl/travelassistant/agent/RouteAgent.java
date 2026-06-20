package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.client.RouteClient;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;
import org.springframework.stereotype.Component;

@Component
public class RouteAgent {

    private final RouteClient routeClient;

    public RouteAgent(RouteClient routeClient) {
        this.routeClient = routeClient;
    }

    public RoutePlan planRoute(RouteQuery query) {
        return routeClient.planRoute(query);
    }
}
