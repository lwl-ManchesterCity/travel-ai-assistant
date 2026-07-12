package com.lwl.travelassistant.model;

import java.util.List;

public class TripPlan {

    private String city;
    private String startDate;
    private String endDate;
    private List<DayPlan> days;
    private List<WeatherInfo> weatherInfo;
    private String overallSuggestions;
    private Budget budget;
    private List<String> planningNotes;
    private List<AgentTrace> agentTraces;
    private LoopExecutionSummary loopSummary;
    private List<LoopIterationTrace> loopIterations;

    public TripPlan() {
    }

    public TripPlan(String city,
                    String startDate,
                    String endDate,
                    List<DayPlan> days,
                    List<WeatherInfo> weatherInfo,
                    String overallSuggestions,
                    Budget budget,
                    List<String> planningNotes,
                    List<AgentTrace> agentTraces) {
        this(city, startDate, endDate, days, weatherInfo, overallSuggestions, budget, planningNotes, agentTraces, null, null);
    }

    public TripPlan(String city,
                    String startDate,
                    String endDate,
                    List<DayPlan> days,
                    List<WeatherInfo> weatherInfo,
                    String overallSuggestions,
                    Budget budget,
                    List<String> planningNotes,
                    List<AgentTrace> agentTraces,
                    LoopExecutionSummary loopSummary,
                    List<LoopIterationTrace> loopIterations) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
        this.weatherInfo = weatherInfo;
        this.overallSuggestions = overallSuggestions;
        this.budget = budget;
        this.planningNotes = planningNotes;
        this.agentTraces = agentTraces;
        this.loopSummary = loopSummary;
        this.loopIterations = loopIterations;
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

    public List<DayPlan> getDays() {
        return days;
    }

    public void setDays(List<DayPlan> days) {
        this.days = days;
    }

    public List<WeatherInfo> getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(List<WeatherInfo> weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public String getOverallSuggestions() {
        return overallSuggestions;
    }

    public void setOverallSuggestions(String overallSuggestions) {
        this.overallSuggestions = overallSuggestions;
    }

    public Budget getBudget() {
        return budget;
    }

    public void setBudget(Budget budget) {
        this.budget = budget;
    }

    public List<String> getPlanningNotes() {
        return planningNotes;
    }

    public void setPlanningNotes(List<String> planningNotes) {
        this.planningNotes = planningNotes;
    }

    public List<AgentTrace> getAgentTraces() {
        return agentTraces;
    }

    public void setAgentTraces(List<AgentTrace> agentTraces) {
        this.agentTraces = agentTraces;
    }

    public LoopExecutionSummary getLoopSummary() {
        return loopSummary;
    }

    public void setLoopSummary(LoopExecutionSummary loopSummary) {
        this.loopSummary = loopSummary;
    }

    public List<LoopIterationTrace> getLoopIterations() {
        return loopIterations;
    }

    public void setLoopIterations(List<LoopIterationTrace> loopIterations) {
        this.loopIterations = loopIterations;
    }
}
