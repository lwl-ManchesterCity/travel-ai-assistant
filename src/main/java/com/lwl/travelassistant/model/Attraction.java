package com.lwl.travelassistant.model;

public class Attraction {

    private String name;
    private String address;
    private Location location;
    private int visitDuration;
    private String description;
    private String category;
    private Double rating;
    private String imageUrl;
    private int ticketPrice;

    public Attraction() {
    }

    public Attraction(String name,
                      String address,
                      Location location,
                      int visitDuration,
                      String description,
                      String category,
                      Double rating,
                      String imageUrl,
                      int ticketPrice) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.visitDuration = visitDuration;
        this.description = description;
        this.category = category;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.ticketPrice = ticketPrice;
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

    public int getVisitDuration() {
        return visitDuration;
    }

    public void setVisitDuration(int visitDuration) {
        this.visitDuration = visitDuration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(int ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
