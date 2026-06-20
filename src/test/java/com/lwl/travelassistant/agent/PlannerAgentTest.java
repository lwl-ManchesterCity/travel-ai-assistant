package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionSearchResult;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelSearchResult;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.PlannerInput;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.model.WeatherQueryResult;
import com.lwl.travelassistant.client.impl.RuleBasedRouteClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerAgentTest {

    private final PlannerAgent plannerAgent = new PlannerAgent(new RouteAgent(new RuleBasedRouteClient()));

    @Test
    void shouldBuildStructuredTripPlanFromPlannerInput() {
        TripPlanRequest request = new TripPlanRequest(
                "杭州",
                "2026-07-01",
                "2026-07-03",
                3000,
                List.of("美食", "自然风景"),
                "公共交通",
                "舒适型酒店",
                "希望整体节奏轻松一些，对海鲜过敏"
        );

        AttractionSearchResult attractionResult = new AttractionSearchResult(
                "杭州",
                request.getPreferences(),
                List.of(
                        new Attraction("西湖", "杭州西湖景区", new Location(120.1551, 30.2741), 180, "经典必去", "自然风景", 4.8, null, 60),
                        new Attraction("河坊街", "杭州上城区", new Location(120.1712, 30.2429), 90, "适合夜游", "美食购物", 4.5, null, 0),
                        new Attraction("灵隐寺", "杭州西湖区", new Location(120.1045, 30.2417), 120, "适合安静体验", "人文历史", 4.7, null, 45)
                ),
                "已匹配景点",
                new ProviderMetadata("local-knowledge-base", "本地知识库景点源", true)
        );
        WeatherQueryResult weatherResult = new WeatherQueryResult(
                "杭州",
                request.getStartDate(),
                request.getEndDate(),
                List.of(
                        new WeatherInfo("2026-07-01", "多云到晴", "晴", 28, 22, "东北风", "2级"),
                        new WeatherInfo("2026-07-02", "多云", "晴", 27, 21, "东风", "2级"),
                        new WeatherInfo("2026-07-03", "晴", "晴", 29, 22, "东南风", "3级")
                ),
                "已获取天气",
                new ProviderMetadata("rule-weather-engine", "规则天气引擎", true)
        );
        HotelSearchResult hotelResult = new HotelSearchResult(
                "杭州",
                request.getAccommodation(),
                List.of(
                        new Hotel("舒适型精选酒店", "杭州中心城区", new Location(120.18, 30.25), "500-800元", "4.5", "距离景点1.5公里", "舒适型酒店", 560)
                ),
                "已推荐酒店",
                new ProviderMetadata("rule-hotel-engine", "规则酒店推荐引擎", true)
        );
        PlannerInput plannerInput = new PlannerInput(
                request,
                attractionResult,
                weatherResult,
                hotelResult,
                new PlanningConstraints(true, true, false, false, List.of("轻松节奏", "餐饮避开海鲜")),
                List.of("优先考虑用户偏好：美食、自然风景"),
                "请生成杭州旅行计划"
        );

        TripPlan response = plannerAgent.buildPlan(plannerInput);

        assertEquals(3, response.getDays().size());
        assertEquals("公共交通", response.getDays().get(0).getTransportation());
        assertEquals(3, response.getWeatherInfo().size());
        assertTrue(response.getOverallSuggestions().contains("杭州"));
        assertTrue(response.getBudget().getTotal() > 0);
        assertTrue(response.getBudget().getRequestedBudget() > 0);
        assertEquals("dinner", response.getDays().get(0).getMeals().get(2).getType());
        assertTrue(response.getDays().get(0).getMeals().get(2).getDescription().contains("避开海鲜"));
        RoutePlan routePlan = response.getDays().get(0).getRoutePlan();
        assertTrue(routePlan.getSteps().size() >= 2);
        assertTrue(routePlan.getTotalDistanceKm() > 0);
        assertTrue(response.getDays().get(0).getEstimatedCost() > 0);
    }
}
