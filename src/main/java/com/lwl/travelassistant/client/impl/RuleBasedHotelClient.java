package com.lwl.travelassistant.client.impl;

import com.lwl.travelassistant.client.HotelClient;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelQuery;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.ProviderMetadata;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleBasedHotelClient implements HotelClient {

    private static final ProviderMetadata PROVIDER = new ProviderMetadata(
            "rule-hotel-engine",
            "规则酒店推荐引擎",
            true
    );

    @Override
    public List<Hotel> recommendHotels(HotelQuery query) {
        String city = query.getCity();
        int budgetPerDay = query.getBudget() / query.getDays();
        String accommodation = query.getAccommodation();
        if (budgetPerDay < 500) {
            return List.of(
                    new Hotel("高性价比便捷酒店", city + "地铁沿线", new Location(116.3970, 39.9180), "300-500元", "4.2", "距离景点 2 公里", accommodation, 320)
            );
        }

        if (budgetPerDay < 1000) {
            return List.of(
                    new Hotel("舒适型精选酒店", city + "中心城区", new Location(116.4030, 39.9200), "500-800元", "4.5", "距离景点 1.5 公里", accommodation, 560),
                    new Hotel("城市轻奢酒店", city + "商圈附近", new Location(116.4100, 39.9250), "700-900元", "4.6", "距离景点 2.2 公里", accommodation, 760)
            );
        }

        return List.of(
                new Hotel("品质度假酒店", city + "核心景区附近", new Location(116.4200, 39.9300), "900-1400元", "4.8", "距离景点 1 公里", accommodation, 980)
        );
    }

    @Override
    public ProviderMetadata getMetadata() {
        return PROVIDER;
    }
}
