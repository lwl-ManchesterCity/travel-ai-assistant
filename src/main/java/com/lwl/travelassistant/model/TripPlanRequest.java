package com.lwl.travelassistant.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TripPlanRequest {

    @NotBlank
    private String city;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "开始日期必须是 yyyy-MM-dd 格式")
    private String startDate;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "结束日期必须是 yyyy-MM-dd 格式")
    private String endDate;

    @Min(1)
    private int budget;

    @NotNull
    @NotEmpty
    private List<String> preferences;

    private String transportation;

    @NotBlank
    private String accommodation;

    private String extraRequirements;

    public TripPlanRequest() {
    }

    public TripPlanRequest(String city,
                           String startDate,
                           String endDate,
                           int budget,
                           List<String> preferences,
                           String transportation,
                           String accommodation) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.preferences = preferences;
        this.transportation = transportation;
        this.accommodation = accommodation;
    }

    public TripPlanRequest(String city,
                           String startDate,
                           String endDate,
                           int budget,
                           List<String> preferences,
                           String transportation,
                           String accommodation,
                           String extraRequirements) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.preferences = preferences;
        this.transportation = transportation;
        this.accommodation = accommodation;
        this.extraRequirements = extraRequirements;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getBudget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
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

    public String getExtraRequirements() {
        return extraRequirements;
    }

    public void setExtraRequirements(String extraRequirements) {
        this.extraRequirements = extraRequirements;
    }

    public int calculateDays() {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }
}
