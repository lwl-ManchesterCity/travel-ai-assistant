package com.lwl.travelassistant.client.selector;

import com.lwl.travelassistant.client.HotelClient;
import com.lwl.travelassistant.client.impl.AmapHotelClient;
import com.lwl.travelassistant.client.impl.RuleBasedHotelClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelQuery;
import com.lwl.travelassistant.model.ProviderMetadata;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ConfigurableHotelClient implements HotelClient {

    private final TravelProviderProperties properties;
    private final RuleBasedHotelClient mockClient;
    private final AmapHotelClient amapClient;

    public ConfigurableHotelClient(TravelProviderProperties properties,
                                   RuleBasedHotelClient mockClient,
                                   AmapHotelClient amapClient) {
        this.properties = properties;
        this.mockClient = mockClient;
        this.amapClient = amapClient;
    }

    @Override
    public List<Hotel> recommendHotels(HotelQuery query) {
        return getDelegate().recommendHotels(query);
    }

    @Override
    public ProviderMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    private HotelClient getDelegate() {
        if ("amap".equalsIgnoreCase(properties.getHotel())) {
            return amapClient;
        }
        return mockClient;
    }
}
