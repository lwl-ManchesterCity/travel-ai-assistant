package com.lwl.travelassistant.model;

public class WeatherQuery {

    private String city;
    private String startDate;
    private String endDate;

    public WeatherQuery() {
    }

    public WeatherQuery(String city, String startDate, String endDate) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
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
}
