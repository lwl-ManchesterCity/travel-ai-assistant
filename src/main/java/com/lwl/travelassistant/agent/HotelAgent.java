package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.client.HotelClient;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelQuery;
import com.lwl.travelassistant.model.HotelSearchResult;
import com.lwl.travelassistant.model.TripPlanRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HotelAgent {

    private final HotelClient hotelClient;

    public HotelAgent(HotelClient hotelClient) {
        this.hotelClient = hotelClient;
    }

    public HotelSearchResult recommend(TripPlanRequest request) {
        HotelQuery hotelQuery = new HotelQuery(
                request.getCity(),
                request.getBudget(),
                request.calculateDays(),
                request.getAccommodation()
        );
        List<Hotel> hotels = hotelClient.recommendHotels(hotelQuery);
        return new HotelSearchResult(
                request.getCity(),
                request.getAccommodation(),
                hotels,
                buildSummary(request, hotels),
                hotelClient.getMetadata()
        );
    }

    private String buildSummary(TripPlanRequest request, List<Hotel> hotels) {
        return String.format(
                "已为 %s 推荐 %d 家%s，预算参考 %d 元",
                request.getCity(),
                hotels.size(),
                request.getAccommodation(),
                request.getBudget()
        );
    }
}
