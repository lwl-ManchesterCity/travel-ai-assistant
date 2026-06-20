package com.lwl.travelassistant.client.impl;

import com.lwl.travelassistant.client.AttractionClient;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionQuery;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.service.TravelKnowledgeService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InMemoryAttractionClient implements AttractionClient {

    private static final ProviderMetadata PROVIDER = new ProviderMetadata(
            "local-knowledge-base",
            "本地知识库景点源",
            true
    );

    private final TravelKnowledgeService travelKnowledgeService;

    public InMemoryAttractionClient(TravelKnowledgeService travelKnowledgeService) {
        this.travelKnowledgeService = travelKnowledgeService;
    }

    @Override
    public List<Attraction> findAttractions(AttractionQuery query) {
        return travelKnowledgeService.findAttractions(query.getCity());
    }

    @Override
    public ProviderMetadata getMetadata() {
        return PROVIDER;
    }
}
