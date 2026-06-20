package com.lwl.travelassistant.model;

import java.util.List;

public class TripPlanResponse {

    private String summary;
    private List<Attraction> attractionHighlights;
    private List<DayPlan> days;
    private HotelRecommendation hotelRecommendation;
    private WeatherInfo weatherInfo;
    private BudgetSummary budgetSummary;
    private List<String> planningNotes;

    public TripPlanResponse() {
    }

    public TripPlanResponse(String summary,
                            List<Attraction> attractionHighlights,
                            List<DayPlan> days,
                            HotelRecommendation hotelRecommendation,
                            WeatherInfo weatherInfo,
                            BudgetSummary budgetSummary,
                            List<String> planningNotes) {
        this.summary = summary;
        this.attractionHighlights = attractionHighlights;
        this.days = days;
        this.hotelRecommendation = hotelRecommendation;
        this.weatherInfo = weatherInfo;
        this.budgetSummary = budgetSummary;
        this.planningNotes = planningNotes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Attraction> getAttractionHighlights() {
        return attractionHighlights;
    }

    public void setAttractionHighlights(List<Attraction> attractionHighlights) {
        this.attractionHighlights = attractionHighlights;
    }

    public List<DayPlan> getDays() {
        return days;
    }

    public void setDays(List<DayPlan> days) {
        this.days = days;
    }

    public HotelRecommendation getHotelRecommendation() {
        return hotelRecommendation;
    }

    public void setHotelRecommendation(HotelRecommendation hotelRecommendation) {
        this.hotelRecommendation = hotelRecommendation;
    }

    public WeatherInfo getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(WeatherInfo weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public BudgetSummary getBudgetSummary() {
        return budgetSummary;
    }

    public void setBudgetSummary(BudgetSummary budgetSummary) {
        this.budgetSummary = budgetSummary;
    }

    public List<String> getPlanningNotes() {
        return planningNotes;
    }

    public void setPlanningNotes(List<String> planningNotes) {
        this.planningNotes = planningNotes;
    }
}
