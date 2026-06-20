package com.lwl.travelassistant.model;

import java.util.List;

public class DayPlan {

    private String date;
    private int dayIndex;
    private String description;
    private String transportation;
    private String accommodation;
    private Hotel hotel;
    private List<Attraction> attractions;
    private List<Meal> meals;
    private RoutePlan routePlan;
    private int estimatedCost;
    private List<String> optimizationNotes;

    public DayPlan() {
    }

    public DayPlan(String date,
                   int dayIndex,
                   String description,
                   String transportation,
                   String accommodation,
                   Hotel hotel,
                   List<Attraction> attractions,
                   List<Meal> meals) {
        this.date = date;
        this.dayIndex = dayIndex;
        this.description = description;
        this.transportation = transportation;
        this.accommodation = accommodation;
        this.hotel = hotel;
        this.attractions = attractions;
        this.meals = meals;
    }

    public DayPlan(String date,
                   int dayIndex,
                   String description,
                   String transportation,
                   String accommodation,
                   Hotel hotel,
                   List<Attraction> attractions,
                   List<Meal> meals,
                   RoutePlan routePlan,
                   int estimatedCost,
                   List<String> optimizationNotes) {
        this.date = date;
        this.dayIndex = dayIndex;
        this.description = description;
        this.transportation = transportation;
        this.accommodation = accommodation;
        this.hotel = hotel;
        this.attractions = attractions;
        this.meals = meals;
        this.routePlan = routePlan;
        this.estimatedCost = estimatedCost;
        this.optimizationNotes = optimizationNotes;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransportation() {
        return transportation;
    }

    public void setTransportation(String transportation) {
        this.transportation = transportation;
    }

    public String getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(String accommodation) {
        this.accommodation = accommodation;
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

    public List<Meal> getMeals() {
        return meals;
    }

    public void setMeals(List<Meal> meals) {
        this.meals = meals;
    }

    public RoutePlan getRoutePlan() {
        return routePlan;
    }

    public void setRoutePlan(RoutePlan routePlan) {
        this.routePlan = routePlan;
    }

    public int getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(int estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public List<String> getOptimizationNotes() {
        return optimizationNotes;
    }

    public void setOptimizationNotes(List<String> optimizationNotes) {
        this.optimizationNotes = optimizationNotes;
    }
}
