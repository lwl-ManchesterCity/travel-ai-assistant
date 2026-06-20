package com.lwl.travelassistant.model;

public class WeatherInfo {

    private String date;
    private String dayWeather;
    private String nightWeather;
    private int dayTemp;
    private int nightTemp;
    private String windDirection;
    private String windPower;
    private String source;
    private String sourceNote;

    public WeatherInfo() {
    }

    public WeatherInfo(String date,
                       String dayWeather,
                       String nightWeather,
                       int dayTemp,
                       int nightTemp,
                       String windDirection,
                       String windPower) {
        this.date = date;
        this.dayWeather = dayWeather;
        this.nightWeather = nightWeather;
        this.dayTemp = dayTemp;
        this.nightTemp = nightTemp;
        this.windDirection = windDirection;
        this.windPower = windPower;
    }

    public WeatherInfo(String date,
                       String dayWeather,
                       String nightWeather,
                       int dayTemp,
                       int nightTemp,
                       String windDirection,
                       String windPower,
                       String source,
                       String sourceNote) {
        this.date = date;
        this.dayWeather = dayWeather;
        this.nightWeather = nightWeather;
        this.dayTemp = dayTemp;
        this.nightTemp = nightTemp;
        this.windDirection = windDirection;
        this.windPower = windPower;
        this.source = source;
        this.sourceNote = sourceNote;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayWeather() {
        return dayWeather;
    }

    public void setDayWeather(String dayWeather) {
        this.dayWeather = dayWeather;
    }

    public String getNightWeather() {
        return nightWeather;
    }

    public void setNightWeather(String nightWeather) {
        this.nightWeather = nightWeather;
    }

    public int getDayTemp() {
        return dayTemp;
    }

    public void setDayTemp(int dayTemp) {
        this.dayTemp = dayTemp;
    }

    public int getNightTemp() {
        return nightTemp;
    }

    public void setNightTemp(int nightTemp) {
        this.nightTemp = nightTemp;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindPower() {
        return windPower;
    }

    public void setWindPower(String windPower) {
        this.windPower = windPower;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }
}
