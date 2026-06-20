package com.lwl.travelassistant.model;

import java.util.List;

public class AttractionQuery {

    private String city;
    private List<String> preferences;

    public AttractionQuery() {
    }

    public AttractionQuery(String city, List<String> preferences) {
        this.city = city;
        this.preferences = preferences;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }
}
