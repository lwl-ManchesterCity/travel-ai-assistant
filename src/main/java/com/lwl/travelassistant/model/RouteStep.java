package com.lwl.travelassistant.model;

public class RouteStep {

    private int orderIndex;
    private String fromName;
    private String toName;
    private String transportationMode;
    private double distanceKm;
    private int durationMinutes;
    private String instruction;

    public RouteStep() {
    }

    public RouteStep(int orderIndex,
                     String fromName,
                     String toName,
                     String transportationMode,
                     double distanceKm,
                     int durationMinutes,
                     String instruction) {
        this.orderIndex = orderIndex;
        this.fromName = fromName;
        this.toName = toName;
        this.transportationMode = transportationMode;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
        this.instruction = instruction;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getTransportationMode() {
        return transportationMode;
    }

    public void setTransportationMode(String transportationMode) {
        this.transportationMode = transportationMode;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}
