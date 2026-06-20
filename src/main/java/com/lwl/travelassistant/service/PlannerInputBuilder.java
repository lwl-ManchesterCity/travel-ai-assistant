package com.lwl.travelassistant.service;

import com.lwl.travelassistant.agent.AttractionAgent;
import com.lwl.travelassistant.agent.HotelAgent;
import com.lwl.travelassistant.agent.WeatherAgent;
import com.lwl.travelassistant.exception.TripPlanningException;
import com.lwl.travelassistant.model.AttractionSearchResult;
import com.lwl.travelassistant.model.HotelSearchResult;
import com.lwl.travelassistant.model.PlannerInput;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherQueryResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlannerInputBuilder {

    private final AttractionAgent attractionAgent;
    private final WeatherAgent weatherAgent;
    private final HotelAgent hotelAgent;
    private final RequirementAnalysisService requirementAnalysisService;

    public PlannerInputBuilder(AttractionAgent attractionAgent,
                               WeatherAgent weatherAgent,
                               HotelAgent hotelAgent,
                               RequirementAnalysisService requirementAnalysisService) {
        this.attractionAgent = attractionAgent;
        this.weatherAgent = weatherAgent;
        this.hotelAgent = hotelAgent;
        this.requirementAnalysisService = requirementAnalysisService;
    }

    public PlannerInput build(TripPlanRequest request) {
        AttractionSearchResult attractionResult = attractionAgent.search(request);
        WeatherQueryResult weatherResult = weatherAgent.query(request);
        HotelSearchResult hotelResult = hotelAgent.recommend(request);
        PlanningConstraints constraints = requirementAnalysisService.analyze(request);
        validatePlanningInputs(attractionResult, weatherResult, hotelResult);
        List<String> planningNotes = buildPlanningNotes(request, attractionResult, weatherResult, hotelResult, constraints);

        return new PlannerInput(
                request,
                attractionResult,
                weatherResult,
                hotelResult,
                constraints,
                planningNotes,
                buildPlannerQuery(request, attractionResult, weatherResult, hotelResult, constraints)
        );
    }

    private void validatePlanningInputs(AttractionSearchResult attractionResult,
                                        WeatherQueryResult weatherResult,
                                        HotelSearchResult hotelResult) {
        if (attractionResult.getAttractions() == null || attractionResult.getAttractions().isEmpty()) {
            throw new TripPlanningException("当前条件下没有找到可用景点，建议更换目的地或调整偏好");
        }
        if (weatherResult.getWeatherInfo() == null || weatherResult.getWeatherInfo().isEmpty()) {
            throw new TripPlanningException("天气信息暂时不可用");
        }
        if (hotelResult.getHotels() == null || hotelResult.getHotels().isEmpty()) {
            throw new TripPlanningException("酒店推荐结果暂时不可用");
        }
    }

    private List<String> buildPlanningNotes(TripPlanRequest request,
                                            AttractionSearchResult attractionResult,
                                            WeatherQueryResult weatherResult,
                                            HotelSearchResult hotelResult,
                                            PlanningConstraints constraints) {
        List<String> notes = new ArrayList<>();
        notes.add("本次规划目的地为" + request.getCity() + "，共" + request.calculateDays() + "天");
        notes.add(attractionResult.getSummary());
        notes.add(weatherResult.getSummary());
        notes.add(hotelResult.getSummary());
        notes.add("景点来源：" + attractionResult.getProvider().getProviderName());
        notes.add("天气来源：" + weatherResult.getProvider().getProviderName());
        notes.add("酒店来源：" + hotelResult.getProvider().getProviderName());
        if (constraints.getAnalysisSource() != null && !constraints.getAnalysisSource().isBlank()) {
            notes.add("需求理解来源：" + constraints.getAnalysisSource());
        }
        if (request.getExtraRequirements() != null && !request.getExtraRequirements().isBlank()) {
            notes.add("额外要求：" + request.getExtraRequirements());
        }
        if (constraints.getExtractedTags() != null && !constraints.getExtractedTags().isEmpty()) {
            notes.add("识别出的规划约束：" + String.join("、", constraints.getExtractedTags()));
        }
        return notes;
    }

    private String buildPlannerQuery(TripPlanRequest request,
                                     AttractionSearchResult attractionResult,
                                     WeatherQueryResult weatherResult,
                                     HotelSearchResult hotelResult,
                                     PlanningConstraints constraints) {
        StringBuilder builder = new StringBuilder();
        builder.append("请为 ").append(request.getCity())
                .append(" 生成 ").append(request.calculateDays()).append(" 天旅行计划。");
        builder.append(" 用户偏好：").append(String.join("、", request.getPreferences())).append("。");
        builder.append(" 出行方式：")
                .append(request.getTransportation() == null || request.getTransportation().isBlank() ? "智能推荐" : request.getTransportation())
                .append("，住宿偏好：")
                .append(request.getAccommodation()).append("。");
        builder.append(" 景点候选数：").append(attractionResult.getAttractions().size()).append("，");
        builder.append(" 天气天数：").append(weatherResult.getWeatherInfo().size()).append("，");
        builder.append(" 酒店候选数：").append(hotelResult.getHotels().size()).append("。");
        if (constraints.getExtractedTags() != null && !constraints.getExtractedTags().isEmpty()) {
            builder.append(" 额外约束：").append(String.join("、", constraints.getExtractedTags())).append("。");
        }
        return builder.toString();
    }
}
