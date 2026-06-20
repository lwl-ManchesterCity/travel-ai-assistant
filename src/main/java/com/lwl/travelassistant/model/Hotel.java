package com.lwl.travelassistant.model;

public class Hotel {

    private String name;
    private String address;
    private Location location;
    private String priceRange;
    private String rating;
    private String distance;
    private String type;
    private int estimatedCost;

    public Hotel() {
    }

    public Hotel(String name,
                 String address,
                 Location location,
                 String priceRange,
                 String rating,
                 String distance,
                 String type,
                 int estimatedCost) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.priceRange = priceRange;
        this.rating = rating;
        this.distance = distance;
        this.type = type;
        this.estimatedCost = estimatedCost;
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

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(int estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
}
