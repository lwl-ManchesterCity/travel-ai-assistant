package com.lwl.travelassistant.client;

import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionQuery;
import com.lwl.travelassistant.model.ProviderMetadata;

import java.util.List;

public interface AttractionClient {

    List<Attraction> findAttractions(AttractionQuery query);

    ProviderMetadata getMetadata();
}
