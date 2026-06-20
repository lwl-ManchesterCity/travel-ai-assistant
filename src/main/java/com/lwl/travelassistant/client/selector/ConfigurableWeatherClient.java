package com.lwl.travelassistant.client.selector;

import com.lwl.travelassistant.client.WeatherClient;
import com.lwl.travelassistant.client.impl.AmapWeatherClient;
import com.lwl.travelassistant.client.impl.RuleBasedWeatherClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.model.WeatherQuery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class ConfigurableWeatherClient implements WeatherClient {

    private final TravelProviderProperties properties;
    private final RuleBasedWeatherClient mockClient;
    private final AmapWeatherClient amapClient;

    public ConfigurableWeatherClient(TravelProviderProperties properties,
                                     RuleBasedWeatherClient mockClient,
                                     AmapWeatherClient amapClient) {
        this.properties = properties;
        this.mockClient = mockClient;
        this.amapClient = amapClient;
    }

    @Override
    public List<WeatherInfo> getWeather(WeatherQuery query) {
        return getDelegate().getWeather(query);
    }

    @Override
    public ProviderMetadata getMetadata() {
        return getDelegate().getMetadata();
    }

    private WeatherClient getDelegate() {
        if ("amap".equalsIgnoreCase(properties.getWeather())) {
            return amapClient;
        }
        return mockClient;
    }
}
