package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.client.WeatherClient;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherQuery;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.model.WeatherQueryResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeatherAgent {

    private final WeatherClient weatherClient;

    public WeatherAgent(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    public WeatherQueryResult query(TripPlanRequest request) {
        WeatherQuery weatherQuery = new WeatherQuery(request.getCity(), request.getStartDate(), request.getEndDate());
        List<WeatherInfo> weatherInfo = weatherClient.getWeather(weatherQuery);
        return new WeatherQueryResult(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                weatherInfo,
                buildSummary(weatherInfo),
                weatherClient.getMetadata()
        );
    }

    private String buildSummary(List<WeatherInfo> weatherInfo) {
        if (weatherInfo == null || weatherInfo.isEmpty()) {
            return "未获取到天气信息";
        }
        WeatherInfo firstDay = weatherInfo.get(0);
        return String.format(
                "已获取 %d 天天气，首日为 %s，白天 %d°C",
                weatherInfo.size(),
                firstDay.getDayWeather(),
                firstDay.getDayTemp()
        );
    }
}
