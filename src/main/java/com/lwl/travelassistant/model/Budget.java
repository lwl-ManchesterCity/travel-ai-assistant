package com.lwl.travelassistant.model;

public class Budget {

    private int totalAttractions;
    private int totalHotels;
    private int totalMeals;
    private int totalTransportation;
    private int total;
    private int requestedBudget;
    private int remainingBudget;
    private String status;
    private boolean withinBudget;

    public Budget() {
    }

    public Budget(int totalAttractions, int totalHotels, int totalMeals, int totalTransportation, int total) {
        this.totalAttractions = totalAttractions;
        this.totalHotels = totalHotels;
        this.totalMeals = totalMeals;
        this.totalTransportation = totalTransportation;
        this.total = total;
    }

    public Budget(int totalAttractions,
                  int totalHotels,
                  int totalMeals,
                  int totalTransportation,
                  int total,
                  int requestedBudget,
                  int remainingBudget,
                  String status,
                  boolean withinBudget) {
        this.totalAttractions = totalAttractions;
        this.totalHotels = totalHotels;
        this.totalMeals = totalMeals;
        this.totalTransportation = totalTransportation;
        this.total = total;
        this.requestedBudget = requestedBudget;
        this.remainingBudget = remainingBudget;
        this.status = status;
        this.withinBudget = withinBudget;
    }

    public int getTotalAttractions() {
        return totalAttractions;
    }

    public void setTotalAttractions(int totalAttractions) {
        this.totalAttractions = totalAttractions;
    }

    public int getTotalHotels() {
        return totalHotels;
    }

    public void setTotalHotels(int totalHotels) {
        this.totalHotels = totalHotels;
    }

    public int getTotalMeals() {
        return totalMeals;
    }

    public void setTotalMeals(int totalMeals) {
        this.totalMeals = totalMeals;
    }

    public int getTotalTransportation() {
        return totalTransportation;
    }

    public void setTotalTransportation(int totalTransportation) {
        this.totalTransportation = totalTransportation;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getRequestedBudget() {
        return requestedBudget;
    }

    public void setRequestedBudget(int requestedBudget) {
        this.requestedBudget = requestedBudget;
    }

    public int getRemainingBudget() {
        return remainingBudget;
    }

    public void setRemainingBudget(int remainingBudget) {
        this.remainingBudget = remainingBudget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isWithinBudget() {
        return withinBudget;
    }

    public void setWithinBudget(boolean withinBudget) {
        this.withinBudget = withinBudget;
    }
}
