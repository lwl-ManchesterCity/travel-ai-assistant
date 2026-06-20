package com.lwl.travelassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TravelRuntimeLogger {

    private static final Logger log = LoggerFactory.getLogger(TravelRuntimeLogger.class);

    @Bean
    public ApplicationRunner logTravelRuntime(TravelProviderProperties properties) {
        return args -> log.info(
                "Travel runtime config -> java.version={}, attraction.provider={}, weather.provider={}, hotel.provider={}, route.provider={}, amap.baseUrl={}, amap.mockResponse={}, amap.curlFallbackEnabled={}, amap.webJsEnabled={}",
                System.getProperty("java.version"),
                properties.getAttraction(),
                properties.getWeather(),
                properties.getHotel(),
                properties.getRoute(),
                properties.getAmap().getBaseUrl(),
                properties.getAmap().isMockResponse(),
                properties.getAmap().isCurlFallbackEnabled(),
                properties.getAmap().getWebJsKey() != null && !properties.getAmap().getWebJsKey().isBlank()
        );
    }
}
