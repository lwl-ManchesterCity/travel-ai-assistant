package com.lwl.travelassistant.model;

import java.util.List;

public class PlannerInput {

    private TripPlanRequest request;
    private AttractionSearchResult attractionResult;
    private WeatherQueryResult weatherResult;
    private HotelSearchResult hotelResult;
    private PlanningConstraints constraints;
    private List<String> planningNotes;
    private String plannerQuery;
    private List<AgentTrace> agentTraces;

    public PlannerInput() {
    }

    public PlannerInput(TripPlanRequest request,
                        AttractionSearchResult attractionResult,
                        WeatherQueryResult weatherResult,
                        HotelSearchResult hotelResult,
                        PlanningConstraints constraints,
                        List<String> planningNotes,
                        String plannerQuery,
                        List<AgentTrace> agentTraces) {
        this.request = request;
        this.attractionResult = attractionResult;
        this.weatherResult = weatherResult;
        this.hotelResult = hotelResult;
        this.constraints = constraints;
        this.planningNotes = planningNotes;
        this.plannerQuery = plannerQuery;
        this.agentTraces = agentTraces;
    }

    public TripPlanRequest getRequest() {
        return request;
    }

    public void setRequest(TripPlanRequest request) {
        this.request = request;
    }

    public AttractionSearchResult getAttractionResult() {
        return attractionResult;
    }

    public void setAttractionResult(AttractionSearchResult attractionResult) {
        this.attractionResult = attractionResult;
    }

    public WeatherQueryResult getWeatherResult() {
        return weatherResult;
    }

    public void setWeatherResult(WeatherQueryResult weatherResult) {
        this.weatherResult = weatherResult;
    }

    public HotelSearchResult getHotelResult() {
        return hotelResult;
    }

    public void setHotelResult(HotelSearchResult hotelResult) {
        this.hotelResult = hotelResult;
    }

    public PlanningConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(PlanningConstraints constraints) {
        this.constraints = constraints;
    }

    public List<String> getPlanningNotes() {
        return planningNotes;
    }

    public void setPlanningNotes(List<String> planningNotes) {
        this.planningNotes = planningNotes;
    }

    public String getPlannerQuery() {
        return plannerQuery;
    }

    public void setPlannerQuery(String plannerQuery) {
        this.plannerQuery = plannerQuery;
    }

    public List<AgentTrace> getAgentTraces() {
        return agentTraces;
    }

    public void setAgentTraces(List<AgentTrace> agentTraces) {
        this.agentTraces = agentTraces;
    }
}
