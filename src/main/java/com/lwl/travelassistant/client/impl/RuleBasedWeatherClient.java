package com.lwl.travelassistant.client.impl;

import com.lwl.travelassistant.client.WeatherClient;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.WeatherQuery;
import com.lwl.travelassistant.model.WeatherInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class RuleBasedWeatherClient implements WeatherClient {

    private static final ProviderMetadata PROVIDER = new ProviderMetadata(
            "rule-weather-engine",
            "规则天气引擎",
            true
    );

    @Override
    public List<WeatherInfo> getWeather(WeatherQuery query) {
        LocalDate start = LocalDate.parse(query.getStartDate());
        LocalDate end = LocalDate.parse(query.getEndDate());
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        return IntStream.range(0, days)
                .mapToObj(offset -> buildWeather(start.plusDays(offset)))
                .toList();
    }

    @Override
    public ProviderMetadata getMetadata() {
        return PROVIDER;
    }

    private WeatherInfo buildWeather(LocalDate date) {
        int month = date.getMonthValue();
        if (month >= 6 && month <= 9) {
            return new WeatherInfo(date.toString(), "晴到多云", "局部阵雨", 32, 25, "东南风", "3级",
                    "rule-fallback", "规则天气补全");
        }
        if (month >= 12 || month <= 2) {
            return new WeatherInfo(date.toString(), "多云", "晴", 11, 3, "北风", "2级",
                    "rule-fallback", "规则天气补全");
        }
        return new WeatherInfo(date.toString(), "多云到晴", "晴", 24, 16, "东北风", "2级",
                "rule-fallback", "规则天气补全");
    }
}
