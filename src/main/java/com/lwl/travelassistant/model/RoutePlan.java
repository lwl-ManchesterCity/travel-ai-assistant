package com.lwl.travelassistant.model;

import java.util.List;

public class RoutePlan {

    private String city;
    private int dayIndex;
    private MapPoint startPoint;
    private MapPoint endPoint;
    private List<RouteStep> steps;
    private double totalDistanceKm;
    private int totalDurationMinutes;
    private String transportationMode;
    private String summary;
    private ProviderMetadata provider;

    public RoutePlan() {
    }

    public RoutePlan(String city,
                     int dayIndex,
                     MapPoint startPoint,
                     MapPoint endPoint,
                     List<RouteStep> steps,
                     double totalDistanceKm,
                     int totalDurationMinutes,
                     String transportationMode,
                     String summary,
                     ProviderMetadata provider) {
        this.city = city;
        this.dayIndex = dayIndex;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.steps = steps;
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationMinutes = totalDurationMinutes;
        this.transportationMode = transportationMode;
        this.summary = summary;
        this.provider = provider;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public MapPoint getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(MapPoint startPoint) {
        this.startPoint = startPoint;
    }

    public MapPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(MapPoint endPoint) {
        this.endPoint = endPoint;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RouteStep> steps) {
        this.steps = steps;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(int totalDurationMinutes) {
        this.totalDurationMinutes = totalDurationMinutes;
    }

    public String getTransportationMode() {
        return transportationMode;
    }

    public void setTransportationMode(String transportationMode) {
        this.transportationMode = transportationMode;
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
