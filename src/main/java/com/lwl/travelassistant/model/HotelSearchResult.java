package com.lwl.travelassistant.model;

import java.util.List;

public class HotelSearchResult {

    private String city;
    private String accommodation;
    private List<Hotel> hotels;
    private String summary;
    private ProviderMetadata provider;

    public HotelSearchResult() {
    }

    public HotelSearchResult(String city,
                             String accommodation,
                             List<Hotel> hotels,
                             String summary,
                             ProviderMetadata provider) {
        this.city = city;
        this.accommodation = accommodation;
        this.hotels = hotels;
        this.summary = summary;
        this.provider = provider;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(String accommodation) {
        this.accommodation = accommodation;
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    public void setHotels(List<Hotel> hotels) {
        this.hotels = hotels;
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
