package com.lwl.travelassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.model.Budget;
import com.lwl.travelassistant.model.DayPlan;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.Meal;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.service.TripPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripPlanController.class)
class TripPlanControllerTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripPlanningService tripPlanningService;

    @Test
    void shouldReturnTripPlanWhenRequestIsValid() throws Exception {
        TripPlanRequest request = new TripPlanRequest(
                "杭州",
                "2026-07-01",
                "2026-07-03",
                3000,
                List.of("美食", "自然风景"),
                "公共交通",
                "舒适型酒店",
                "希望节奏轻松一点"
        );

        TripPlan response = new TripPlan(
                "杭州",
                "2026-07-01",
                "2026-07-03",
                List.of(new DayPlan(
                        "2026-07-01",
                        0,
                        "第1天围绕西湖展开。",
                        "公共交通",
                        "舒适型酒店",
                        new Hotel("舒适型精选酒店", "杭州中心城区", new Location(120.18, 30.25), "500-800元", "4.5", "距离景点1.5公里", "舒适型酒店", 560),
                        List.of(),
                        List.of(new Meal("breakfast", "杭州风味早餐", "西湖边", new Location(120.15, 30.27), "轻量开启当天行程", 30))
                )),
                List.of(new WeatherInfo("2026-07-01", "多云到晴", "晴", 28, 22, "东北风", "2级")),
                "杭州 3 天旅行计划",
                new Budget(150, 1200, 540, 300, 2190)
        );

        when(tripPlanningService.planTrip(any(TripPlanRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallSuggestions").value("杭州 3 天旅行计划"))
                .andExpect(jsonPath("$.days[0].transportation").value("公共交通"))
                .andExpect(jsonPath("$.budget.total").value(2190));
    }

    @Test
    void shouldReturnValidationErrorWhenRequestIsInvalid() throws Exception {
        String invalidRequest = """
                {
                  "city": "",
                  "startDate": "2026/07/01",
                  "endDate": "",
                  "budget": 0,
                  "preferences": [],
                  "transportation": "",
                  "accommodation": ""
                }
                """;

        mockMvc.perform(post("/api/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请求参数校验失败"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
