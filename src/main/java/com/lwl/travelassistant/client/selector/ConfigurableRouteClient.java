package com.lwl.travelassistant.client.selector;

import com.lwl.travelassistant.client.RouteClient;
import com.lwl.travelassistant.client.impl.AmapRouteClient;
import com.lwl.travelassistant.client.impl.RuleBasedRouteClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ConfigurableRouteClient implements RouteClient {

    private final TravelProviderProperties properties;
    private final RuleBasedRouteClient mockClient;
    private final AmapRouteClient amapClient;

    public ConfigurableRouteClient(TravelProviderProperties properties,
                                   RuleBasedRouteClient mockClient,
                                   AmapRouteClient amapClient) {
        this.properties = properties;
        this.mockClient = mockClient;
        this.amapClient = amapClient;
    }

    @Override
    public RoutePlan planRoute(RouteQuery query) {
        return getDelegate().planRoute(query);
    }

    @Override
    public ProviderMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    private RouteClient getDelegate() {
        if ("amap".equalsIgnoreCase(properties.getRoute())) {
            return amapClient;
        }
        return mockClient;
    }
}
