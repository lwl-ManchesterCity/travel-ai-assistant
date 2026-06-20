package com.lwl.travelassistant.client;

import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;

public interface RouteClient {

    RoutePlan planRoute(RouteQuery query);

    ProviderMetadata getMetadata();
}
