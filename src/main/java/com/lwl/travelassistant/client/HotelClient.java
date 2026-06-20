package com.lwl.travelassistant.client;

import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelQuery;
import com.lwl.travelassistant.model.ProviderMetadata;

import java.util.List;

public interface HotelClient {

    List<Hotel> recommendHotels(HotelQuery query);

    ProviderMetadata getMetadata();
}
