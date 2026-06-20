package com.lwl.travelassistant.model;

import java.util.List;

public class WeatherQueryResult {

    private String city;
    private String startDate;
    private String endDate;
    private List<WeatherInfo> weatherInfo;
    private String summary;
    private ProviderMetadata provider;

    public WeatherQueryResult() {
    }

    public WeatherQueryResult(String city,
                              String startDate,
                              String endDate,
                              List<WeatherInfo> weatherInfo,
                              String summary,
                              ProviderMetadata provider) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weatherInfo = weatherInfo;
        this.summary = summary;
        this.provider = provider;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<WeatherInfo> getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(List<WeatherInfo> weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ProviderMetadata getProvider() {
        return provider;
    }

    public void setProvider(ProviderMetadata provider) {
        this.provider = provider;
    }
}
