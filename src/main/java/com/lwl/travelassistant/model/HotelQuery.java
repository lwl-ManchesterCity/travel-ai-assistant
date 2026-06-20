package com.lwl.travelassistant.model;

public class HotelQuery {

    private String city;
    private int budget;
    private int days;
    private String accommodation;

    public HotelQuery() {
    }

    public HotelQuery(String city, int budget, int days, String accommodation) {
        this.city = city;
        this.budget = budget;
        this.days = days;
        this.accommodation = accommodation;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public String getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(String accommodation) {
        this.accommodation = accommodation;
    }
}
