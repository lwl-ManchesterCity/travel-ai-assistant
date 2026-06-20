package com.lwl.travelassistant.evaluator;

import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

@Component
public class WeatherEvaluator {

    public boolean shouldPreferIndoor(PlanningConstraints constraints, WeatherInfo currentWeather) {
        if (constraints == null || !constraints.isPrefersIndoorBackup()) {
            return false;
        }
        if (currentWeather == null || currentWeather.getDayWeather() == null) {
            return false;
        }
        String weather = currentWeather.getDayWeather();
        return weather.contains("雨") || weather.contains("雪") || weather.contains("雷");
    }
}
