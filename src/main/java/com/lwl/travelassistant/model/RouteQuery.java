package com.lwl.travelassistant.model;

import java.util.List;

public class RouteQuery {

    private String city;
    private int dayIndex;
    private String transportation;
    private Hotel hotel;
    private List<Attraction> attractions;

    public RouteQuery() {
    }

    public RouteQuery(String city, int dayIndex, String transportation, Hotel hotel, List<Attraction> attractions) {
        this.city = city;
        this.dayIndex = dayIndex;
        this.transportation = transportation;
        this.hotel = hotel;
        this.attractions = attractions;
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

    public String getTransportation() {
        return transportation;
    }

    public void setTransportation(String transportation) {
        this.transportation = transportation;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public List<Attraction> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<Attraction> attractions) {
        this.attractions = attractions;
    }
}
