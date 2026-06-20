package com.lwl.travelassistant.client;

import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.WeatherQuery;
import com.lwl.travelassistant.model.WeatherInfo;

import java.util.List;

public interface WeatherClient {

    List<WeatherInfo> getWeather(WeatherQuery query);

    ProviderMetadata getMetadata();
}
