package com.lwl.travelassistant.model;

public class MapPoint {

    private String name;
    private String address;
    private Location location;
    private String pointType;

    public MapPoint() {
    }

    public MapPoint(String name, String address, Location location, String pointType) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.pointType = pointType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }
}
