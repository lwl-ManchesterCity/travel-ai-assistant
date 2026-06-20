package com.lwl.travelassistant.client.impl;

import com.lwl.travelassistant.client.RouteClient;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.MapPoint;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;
import com.lwl.travelassistant.model.RouteStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleBasedRouteClient implements RouteClient {

    private static final ProviderMetadata PROVIDER = new ProviderMetadata(
            "rule-route-engine",
            "规则路线规划引擎",
            true
    );

    @Override
    public RoutePlan planRoute(RouteQuery query) {
        Hotel hotel = query.getHotel();
        List<Attraction> attractions = query.getAttractions();
        List<RouteStep> steps = new ArrayList<>();
        MapPoint startPoint = buildHotelPoint(hotel);
        MapPoint currentPoint = startPoint;
        double totalDistanceKm = 0;
        int totalDurationMinutes = 0;

        for (int index = 0; index < attractions.size(); index++) {
            Attraction attraction = attractions.get(index);
            MapPoint nextPoint = buildAttractionPoint(attraction);
            double distanceKm = estimateDistance(currentPoint.getLocation(), nextPoint.getLocation());
            int durationMinutes = estimateDuration(distanceKm, query.getTransportation());
            totalDistanceKm += distanceKm;
            totalDurationMinutes += durationMinutes;
            steps.add(new RouteStep(
                    index + 1,
                    currentPoint.getName(),
                    nextPoint.getName(),
                    query.getTransportation(),
                    round(distanceKm),
                    durationMinutes,
                    buildInstruction(currentPoint.getName(), nextPoint.getName(), query.getTransportation(), distanceKm)
            ));
            currentPoint = nextPoint;
        }

        if (hotel != null && currentPoint.getLocation() != null) {
            double backDistanceKm = estimateDistance(currentPoint.getLocation(), hotel.getLocation());
            int backDurationMinutes = estimateDuration(backDistanceKm, query.getTransportation());
            totalDistanceKm += backDistanceKm;
            totalDurationMinutes += backDurationMinutes;
            steps.add(new RouteStep(
                    steps.size() + 1,
                    currentPoint.getName(),
                    hotel.getName(),
                    query.getTransportation(),
                    round(backDistanceKm),
                    backDurationMinutes,
                    buildInstruction(currentPoint.getName(), hotel.getName(), query.getTransportation(), backDistanceKm)
            ));
        }

        MapPoint endPoint = hotel != null ? buildHotelPoint(hotel) : currentPoint;
        return new RoutePlan(
                query.getCity(),
                query.getDayIndex(),
                startPoint,
                endPoint,
                steps,
                round(totalDistanceKm),
                totalDurationMinutes,
                query.getTransportation(),
                buildSummary(query, totalDistanceKm, totalDurationMinutes, steps.size()),
                PROVIDER
        );
    }

    @Override
    public ProviderMetadata getMetadata() {
        return PROVIDER;
    }

    private MapPoint buildHotelPoint(Hotel hotel) {
        if (hotel == null) {
            return new MapPoint("市中心出发点", "默认出发点", new Location(0, 0), "hotel");
        }
        return new MapPoint(hotel.getName(), hotel.getAddress(), hotel.getLocation(), "hotel");
    }

    private MapPoint buildAttractionPoint(Attraction attraction) {
        return new MapPoint(attraction.getName(), attraction.getAddress(), attraction.getLocation(), "attraction");
    }

    private String buildInstruction(String from, String to, String transportation, double distanceKm) {
        return String.format("从%s前往%s，建议使用%s，约 %.1f 公里。", from, to, transportation, round(distanceKm));
    }

    private String buildSummary(RouteQuery query, double totalDistanceKm, int totalDurationMinutes, int stepCount) {
        return String.format(
                "第%d天路线已生成，共 %d 段，预计 %.1f 公里，通勤约 %d 分钟。",
                query.getDayIndex() + 1,
                stepCount,
                round(totalDistanceKm),
                totalDurationMinutes
        );
    }

    private int estimateDuration(double distanceKm, String transportation) {
        double speedKmPerHour;
        if (transportation != null && transportation.contains("步行")) {
            speedKmPerHour = 5;
        } else if (transportation != null && transportation.contains("打车")) {
            speedKmPerHour = 28;
        } else {
            speedKmPerHour = 18;
        }
        return Math.max(10, (int) Math.round(distanceKm / speedKmPerHour * 60));
    }

    private double estimateDistance(Location from, Location to) {
        if (from == null || to == null) {
            return 3.0;
        }
        double latDiff = from.getLatitude() - to.getLatitude();
        double lngDiff = from.getLongitude() - to.getLongitude();
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
