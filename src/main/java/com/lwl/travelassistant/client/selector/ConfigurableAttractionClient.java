package com.lwl.travelassistant.client.selector;

import com.lwl.travelassistant.client.AttractionClient;
import com.lwl.travelassistant.client.impl.AmapAttractionClient;
import com.lwl.travelassistant.client.impl.InMemoryAttractionClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionQuery;
import com.lwl.travelassistant.model.ProviderMetadata;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ConfigurableAttractionClient implements AttractionClient {

    private final TravelProviderProperties properties;
    private final InMemoryAttractionClient mockClient;
    private final AmapAttractionClient amapClient;

    public ConfigurableAttractionClient(TravelProviderProperties properties,
                                        InMemoryAttractionClient mockClient,
                                        AmapAttractionClient amapClient) {
        this.properties = properties;
        this.mockClient = mockClient;
        this.amapClient = amapClient;
    }

    @Override
    public List<Attraction> findAttractions(AttractionQuery query) {
        return getDelegate().findAttractions(query);
    }

    @Override
    public ProviderMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    private AttractionClient getDelegate() {
        if ("amap".equalsIgnoreCase(properties.getAttraction())) {
            return amapClient;
        }
        return mockClient;
    }
}
