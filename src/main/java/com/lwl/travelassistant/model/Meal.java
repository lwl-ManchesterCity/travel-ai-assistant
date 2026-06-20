package com.lwl.travelassistant.model;

public class Meal {

    private String type;
    private String name;
    private String address;
    private Location location;
    private String description;
    private int estimatedCost;

    public Meal() {
    }

    public Meal(String type, String name, String address, Location location, String description, int estimatedCost) {
        this.type = type;
        this.name = name;
        this.address = address;
        this.location = location;
        this.description = description;
        this.estimatedCost = estimatedCost;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(int estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}
