package com.lwl.travelassistant.model;

import java.util.List;

public class AttractionSearchResult {

    private String city;
    private List<String> preferences;
    private List<Attraction> attractions;
    private String summary;
    private ProviderMetadata provider;

    public AttractionSearchResult() {
    }

    public AttractionSearchResult(String city,
                                  List<String> preferences,
                                  List<Attraction> attractions,
                                  String summary,
                                  ProviderMetadata provider) {
        this.city = city;
        this.preferences = preferences;
        this.attractions = attractions;
        this.summary = summary;
        this.provider = provider;
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

    public List<Attraction> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<Attraction> attractions) {
        this.attractions = attractions;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ProviderMetadata getProvider() {
        return provider;
    }

    public void setProvider(ProviderMetadata provider) {
        this.provider = provider;
    }
}
