package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.client.AttractionClient;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionQuery;
import com.lwl.travelassistant.model.AttractionSearchResult;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AttractionAgent {

    private final AttractionClient attractionClient;

    public AttractionAgent(AttractionClient attractionClient) {
        this.attractionClient = attractionClient;
    }

    public AttractionSearchResult search(TripPlanRequest request) {
        List<Attraction> matchedAttractions = searchAttractions(request);
        return new AttractionSearchResult(
                request.getCity(),
                request.getPreferences(),
                matchedAttractions,
                buildSummary(request, matchedAttractions),
                attractionClient.getMetadata()
        );
    }

    private List<Attraction> searchAttractions(TripPlanRequest request) {
        AttractionQuery query = new AttractionQuery(request.getCity(), request.getPreferences());
        List<Attraction> candidateAttractions = attractionClient.findAttractions(query);
        List<Attraction> matchedAttractions = new ArrayList<>();

        for (String preference : request.getPreferences()) {
            String normalizedPreference = preference.toLowerCase(Locale.ROOT);
            for (Attraction attraction : candidateAttractions) {
                if (matchesPreference(attraction, normalizedPreference) && !containsByName(matchedAttractions, attraction.getName())) {
                    matchedAttractions.add(attraction);
                }
            }
        }

        if (matchedAttractions.isEmpty()) {
            return candidateAttractions;
        }

        for (Attraction attraction : candidateAttractions) {
            if (!containsByName(matchedAttractions, attraction.getName())) {
                matchedAttractions.add(attraction);
            }
        }

        return matchedAttractions;
    }

    private String buildSummary(TripPlanRequest request, List<Attraction> attractions) {
        return String.format(
                "已为 %s 匹配到 %d 个候选景点，优先偏好为：%s",
                request.getCity(),
                attractions.size(),
                String.join("、", request.getPreferences())
        );
    }

    private boolean matchesPreference(Attraction attraction, String preference) {
        return attraction.getCategory().toLowerCase(Locale.ROOT).contains(preference)
                || attraction.getDescription().toLowerCase(Locale.ROOT).contains(preference);
    }

    private boolean containsByName(List<Attraction> attractions, String attractionName) {
        for (Attraction attraction : attractions) {
            if (attraction.getName().equals(attractionName)) {
                return true;
            }
        }
        return false;
    }
}
