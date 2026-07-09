package com.lwl.travelassistant.agent;

import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AgentTrace;
import com.lwl.travelassistant.model.Budget;
import com.lwl.travelassistant.model.DailyPlanEvaluation;
import com.lwl.travelassistant.model.DailyPlanReflection;
import com.lwl.travelassistant.model.DayPlan;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.Meal;
import com.lwl.travelassistant.model.PlanningConstraints;
import com.lwl.travelassistant.model.PlannerInput;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;
import com.lwl.travelassistant.model.TripPlan;
import com.lwl.travelassistant.model.TripPlanRequest;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.service.LlmNarrationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class PlannerAgent {

    private final RouteAgent routeAgent;
    private final ReflectionAgent reflectionAgent;
    private final LlmNarrationService llmNarrationService;

    public PlannerAgent(RouteAgent routeAgent,
                        ReflectionAgent reflectionAgent,
                        LlmNarrationService llmNarrationService) {
        this.routeAgent = routeAgent;
        this.reflectionAgent = reflectionAgent;
        this.llmNarrationService = llmNarrationService;
    }

    public TripPlan buildPlan(PlannerInput plannerInput) {
        TripPlanRequest request = plannerInput.getRequest();
        List<Attraction> attractions = sanitizeAttractions(plannerInput.getAttractionResult().getAttractions());
        List<WeatherInfo> weatherInfo = plannerInput.getWeatherResult().getWeatherInfo();
        List<Hotel> hotels = plannerInput.getHotelResult().getHotels();
        PlanningConstraints constraints = plannerInput.getConstraints();
        List<DayPlan> dayPlans = new ArrayList<>();
        List<AgentTrace> agentTraces = plannerInput.getAgentTraces() == null
                ? new ArrayList<>()
                : new ArrayList<>(plannerInput.getAgentTraces());
        Set<String> usedAttractionNames = new HashSet<>();
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        int dailyBudgetLimit = Math.max(200, request.getBudget() / request.calculateDays());

        for (int dayIndex = 0; dayIndex < request.calculateDays(); dayIndex++) {
            String currentDate = startDate.plusDays(dayIndex).toString();
            WeatherInfo currentWeather = findWeatherForDate(weatherInfo, currentDate);
            List<Attraction> preliminaryAttractions = pickDailyAttractions(
                    attractions,
                    dayIndex,
                    dailyBudgetLimit,
                    null,
                    usedAttractionNames,
                    request.getPreferences(),
                    constraints,
                    currentWeather
            );
            Hotel selectedHotel = selectHotel(hotels, preliminaryAttractions, dailyBudgetLimit);
            List<Attraction> dailyAttractions = pickDailyAttractions(
                    attractions,
                    dayIndex,
                    dailyBudgetLimit,
                    selectedHotel,
                    usedAttractionNames,
                    request.getPreferences(),
                    constraints,
                    currentWeather
            );
            DailyPlanDecision decision = refineDailyPlan(
                    request,
                    constraints,
                    hotels,
                    attractions,
                    usedAttractionNames,
                    agentTraces,
                    dayIndex,
                    dailyBudgetLimit,
                    currentWeather,
                    selectedHotel,
                    dailyAttractions
            );
            markAttractionsAsUsed(usedAttractionNames, decision.attractions);
            Attraction leadAttraction = decision.attractions.get(0);
            DayPlan dayPlan = new DayPlan(
                    currentDate,
                    dayIndex,
                    buildDescription(leadAttraction, constraints, decision.routePlan, decision.estimatedCost, dayIndex + 1, decision.notes),
                    decision.transportation,
                    request.getAccommodation(),
                    decision.hotel,
                    decision.attractions,
                    decision.meals,
                    decision.routePlan,
                    decision.estimatedCost,
                    decision.notes
            );
            dayPlan.setDescription(llmNarrationService.polishDayDescription(request, dayPlan));
            dayPlans.add(dayPlan);
        }

        List<String> planningNotes = plannerInput.getPlanningNotes() == null
                ? new ArrayList<>()
                : new ArrayList<>(plannerInput.getPlanningNotes());
        if (llmNarrationService.isAvailable()) {
            planningNotes.add("结果文案来源：LLM润色");
        } else {
            planningNotes.add("结果文案来源：规则文案");
        }
        planningNotes.add("ReflectionAgent 已对每日方案完成预算、路线、天气反思评估");

        String overallSuggestions = buildOverallSuggestions(request, weatherInfo, planningNotes, dayPlans);
        overallSuggestions = llmNarrationService.polishOverallSuggestions(
                request,
                weatherInfo,
                dayPlans,
                planningNotes,
                overallSuggestions
        );

        return new TripPlan(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                dayPlans,
                weatherInfo,
                overallSuggestions,
                buildBudget(dayPlans, request.getBudget()),
                planningNotes,
                agentTraces
        );
    }
    //从候选景点里，挑出“今天要去的景点”
    private List<Attraction> pickDailyAttractions(List<Attraction> attractions,
                                                  int dayIndex,
                                                  int dailyBudgetLimit,
                                                  Hotel hotel,
                                                  Set<String> usedAttractionNames,
                                                  List<String> preferences,
                                                  PlanningConstraints constraints,
                                                  WeatherInfo currentWeather) {
        List<Attraction> selected = new ArrayList<>();
        List<Attraction> nearbyAttractions = filterNearbyAttractions(attractions, hotel);
        int remainingTicketBudget = Math.max(0, dailyBudgetLimit - estimateHotelCost(hotel) - 180);
        int targetCount = determineTargetCount(dayIndex, dailyBudgetLimit, constraints, currentWeather);
        List<Attraction> candidateAttractions = prioritizeCandidateAttractions(
                nearbyAttractions.isEmpty() ? attractions : nearbyAttractions,
                preferences,
                constraints,
                currentWeather,
                dailyBudgetLimit
        );
        trySelectAttractions(selected, candidateAttractions, dayIndex, targetCount, remainingTicketBudget, usedAttractionNames, true);
        if (selected.size() < targetCount) {
            trySelectAttractions(selected, candidateAttractions, dayIndex, targetCount, remainingTicketBudget, usedAttractionNames, false);
        }
        if (selected.isEmpty()) {
            Attraction fallback = findFallbackAttraction(candidateAttractions, usedAttractionNames);
            if (fallback != null) {
                selected.add(fallback);
            }
        }
        return selected;
    }

    private Attraction findFallbackAttraction(List<Attraction> candidateAttractions,
                                              Set<String> usedAttractionNames) {
        for (Attraction candidateAttraction : candidateAttractions) {
            if (!usedAttractionNames.contains(candidateAttraction.getName())) {
                return candidateAttraction;
            }
        }
        return candidateAttractions.isEmpty() ? null : candidateAttractions.get(0);
    }

    private int determineTargetCount(int dayIndex,
                                     int dailyBudgetLimit,
                                     PlanningConstraints constraints,
                                     WeatherInfo currentWeather) {
        int targetCount = dayIndex % 2 == 0 ? 3 : 2;
        if (dailyBudgetLimit >= 2500) {
            targetCount = 4;
        } else if (dailyBudgetLimit >= 1600) {
            targetCount = Math.max(targetCount, 3);
        }
        if (dailyBudgetLimit < 550) {
            targetCount = Math.min(targetCount, 2);
        }
        if (dailyBudgetLimit < 380) {
            targetCount = 1;
        }
        if (constraints != null && constraints.isRelaxedPace()) {
            targetCount = Math.min(targetCount, 2);
        }
        if (prefersIndoorDay(constraints, currentWeather)) {
            targetCount = Math.min(targetCount, 2);
        }
        return Math.max(1, targetCount);
    }

    private List<Attraction> prioritizeCandidateAttractions(List<Attraction> attractions,
                                                            List<String> preferences,
                                                            PlanningConstraints constraints,
                                                            WeatherInfo currentWeather,
                                                            int dailyBudgetLimit) {
        List<Attraction> prioritized = new ArrayList<>(attractions);
        prioritized.sort(Comparator
                .comparingDouble((Attraction attraction) ->
                        scoreAttraction(attraction, preferences, constraints, currentWeather, dailyBudgetLimit))
                .reversed()
                .thenComparingInt(Attraction::getTicketPrice));
        return prioritized;
    }

    private double scoreAttraction(Attraction attraction,
                                   List<String> preferences,
                                   PlanningConstraints constraints,
                                   WeatherInfo currentWeather,
                                   int dailyBudgetLimit) {
        double score = 0;
        if (matchesPreference(attraction, preferences)) {
            score += 40;
        }
        if (prefersIndoorDay(constraints, currentWeather)) {
            score += isIndoorFriendly(attraction) ? 18 : -20;
        }
        if (constraints != null && constraints.isRelaxedPace() && attraction.getVisitDuration() <= 150) {
            score += 8;
        }
        if (dailyBudgetLimit < 550 && attraction.getTicketPrice() <= 50) {
            score += 10;
        }
        if (dailyBudgetLimit < 380 && attraction.getTicketPrice() > 60) {
            score -= 12;
        }
        if (dailyBudgetLimit >= 1600 && attraction.getTicketPrice() >= 60) {
            score += 6;
        }
        if (dailyBudgetLimit >= 2500 && attraction.getVisitDuration() >= 150) {
            score += 4;
        }
        if (attraction.getRating() != null) {
            score += attraction.getRating();
        }
        return score;
    }

    private boolean matchesPreference(Attraction attraction, List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return true;
        }
        String category = attraction.getCategory() == null ? "" : attraction.getCategory();
        for (String preference : preferences) {
            if (category.contains(preference)) {
                return true;
            }
        }
        return false;
    }

    private WeatherInfo findWeatherForDate(List<WeatherInfo> weatherInfo, String date) {
        if (weatherInfo == null || weatherInfo.isEmpty()) {
            return null;
        }
        for (WeatherInfo item : weatherInfo) {
            if (date.equals(item.getDate())) {
                return item;
            }
        }
        return weatherInfo.get(0);
    }

    private boolean prefersIndoorDay(PlanningConstraints constraints, WeatherInfo currentWeather) {
       //判断用户根本没有“偏好室内备选”这个要求
        if (constraints == null || !constraints.isPrefersIndoorBackup()) {
            return false;
        }
        if (currentWeather == null || currentWeather.getDayWeather() == null) {
            return false;
        }
        String weather = currentWeather.getDayWeather();
        return weather.contains("雨") || weather.contains("雪") || weather.contains("雷");
    }

    private boolean isIndoorFriendly(Attraction attraction) {
        String category = attraction.getCategory() == null ? "" : attraction.getCategory();
        return "人文历史".equals(category) || "城市漫游".equals(category);
    }

    private void trySelectAttractions(List<Attraction> selected,
                                      List<Attraction> candidateAttractions,
                                      int dayIndex,
                                      int targetCount,
                                      int remainingTicketBudget,
                                      Set<String> usedAttractionNames,
                                      boolean skipUsedAttractions) {
        for (int offset = 0; offset < candidateAttractions.size() && selected.size() < targetCount; offset++) {
            Attraction attraction = candidateAttractions.get((dayIndex + offset) % candidateAttractions.size());
            if (skipUsedAttractions && usedAttractionNames.contains(attraction.getName())) {
                continue;
            }
            int nextTicketTotal = selected.stream().mapToInt(Attraction::getTicketPrice).sum() + attraction.getTicketPrice();
            if (selected.isEmpty() || nextTicketTotal <= remainingTicketBudget) {
                if (!containsAttraction(selected, attraction.getName()) && isReasonablyClose(selected, attraction)) {
                    selected.add(attraction);
                }
            }
        }
    }

    private void markAttractionsAsUsed(Set<String> usedAttractionNames, List<Attraction> dailyAttractions) {
        for (Attraction attraction : dailyAttractions) {
            usedAttractionNames.add(attraction.getName());
        }
    }

    private String buildDescription(Attraction attraction,
                                    PlanningConstraints constraints,
                                    RoutePlan routePlan,
                                    int estimatedCost,
                                    int dayNumber,
                                    List<String> decisionNotes) {
        StringBuilder builder = new StringBuilder("第")
                .append(dayNumber)
                .append("天围绕")
                .append(attraction.getName())
                .append("展开，重点体验")
                .append(attraction.getCategory())
                .append("内容。预计当日花费约 ")
                .append(estimatedCost)
                .append(" 元。");
        if (constraints != null && constraints.isRelaxedPace()) {
            builder.append(" 行程节奏会更宽松，预留更多休息和拍照时间。");
        }
        if (constraints != null && constraints.isWantsLandmarkCeremony()) {
            builder.append(" 如果条件允许，可优先安排带有仪式感的地标打卡。");
        }
        if (constraints != null && constraints.isPrefersIndoorBackup()) {
            builder.append(" 同时预留室内备选方案，方便根据天气灵活调整。");
        }
        if (routePlan != null) {
            builder.append(" ").append(routePlan.getSummary());
        }
        if (decisionNotes != null && !decisionNotes.isEmpty()) {
            builder.append(" 已自动优化：").append(String.join("；", decisionNotes)).append("。");
        }
        return builder.toString();
    }

    private DailyPlanDecision refineDailyPlan(TripPlanRequest request,
                                              PlanningConstraints constraints,
                                              List<Hotel> hotels,
                                              List<Attraction> allAttractions,
                                              Set<String> usedAttractionNames,
                                              List<AgentTrace> agentTraces,
                                              int dayIndex,
                                              int dailyBudgetLimit,
                                              WeatherInfo currentWeather,
                                              Hotel selectedHotel,
                                              List<Attraction> dailyAttractions) {
        Hotel workingHotel = selectedHotel;
        List<Attraction> workingAttractions = new ArrayList<>(dailyAttractions);
        List<String> notes = new ArrayList<>();
        boolean hotelAdjusted = false;
        int adjustRounds = 0;

        while (true) {
            adjustRounds++;
            String transportation = decideTransportation(request, constraints, workingAttractions);
            enforceHardBudgetCuts(workingAttractions, workingHotel, dailyBudgetLimit, notes);
            List<Meal> meals = buildMeals(request.getCity(), request.getPreferences(), constraints, workingAttractions, dailyBudgetLimit);
            RoutePlan routePlan = routeAgent.planRoute(new RouteQuery(
                    request.getCity(),
                    dayIndex,
                    transportation,
                    workingHotel,
                    workingAttractions
            ));
            int estimatedCost = estimateDailyCost(workingAttractions, workingHotel, meals, routePlan);
            agentTraces.add(new AgentTrace(
                    "PlannerAgent",
                    "生成第" + (dayIndex + 1) + "天候选方案",
                    "候选景点=" + joinAttractionNames(workingAttractions) + "，酒店=" + safeHotelName(workingHotel),
                    "预计花费=" + estimatedCost + "元，通勤=" + (routePlan == null ? 0 : routePlan.getTotalDurationMinutes()) + "分钟",
                    "generated"
            ));
            DailyPlanReflection reflection = reflectionAgent.reflectDailyPlan(
                    estimatedCost,
                    dailyBudgetLimit,
                    routePlan,
                    constraints,
                    currentWeather
            );
            DailyPlanEvaluation evaluation = reflection.getEvaluation();
            appendReflectionNotes(notes, reflection.getReflectionNotes());
            agentTraces.add(new AgentTrace(
                    "ReflectionAgent",
                    "反思第" + (dayIndex + 1) + "天候选方案",
                    "预算上限=" + dailyBudgetLimit + "元，天气=" + (currentWeather == null ? "未知" : currentWeather.getDayWeather()),
                    reflection.getReason() + "，建议动作=" + reflection.getRecommendedAction(),
                    reflection.isAcceptable() ? "accepted" : "need_revision"
            ));
            if (reflection.isAcceptable()) {
                return new DailyPlanDecision(workingHotel, workingAttractions, transportation, meals, routePlan, estimatedCost, notes);
            }
            if (adjustRounds >= 6) {
                notes.add("已达到预算压缩上限，当前方案为可生成的最低配版本");
                return new DailyPlanDecision(workingHotel, workingAttractions, transportation, meals, routePlan, estimatedCost, notes);
            }
            if (recommends(reflection, "replace_with_indoor")) {
                appendDiagnosis(notes, evaluation.getWeatherDiagnosis());
                Attraction indoorReplacementTarget = findOutdoorAttractionForReplacement(workingAttractions);
                Attraction indoorAlternative = selectReplacementAttraction(
                        allAttractions,
                        workingAttractions,
                        usedAttractionNames,
                        workingHotel,
                        request.getPreferences(),
                        constraints,
                        currentWeather,
                        dailyBudgetLimit,
                        indoorReplacementTarget,
                        "indoor"
                );
                if (indoorReplacementTarget != null && indoorAlternative != null) {
                    replaceAttraction(workingAttractions, indoorReplacementTarget, indoorAlternative);
                    notes.add("已将 " + indoorReplacementTarget.getName() + " 调整为更适合雨天的 " + indoorAlternative.getName());
                    continue;
                }
            }
            if (recommends(reflection, "reduce_budget") && !hotelAdjusted) {
                appendDiagnosis(notes, evaluation.getBudgetDiagnosis());
                Hotel cheaperHotel = selectBudgetFriendlyHotel(hotels, workingAttractions, dailyBudgetLimit, workingHotel);
                if (cheaperHotel != null && cheaperHotel != workingHotel) {
                    workingHotel = cheaperHotel;
                    hotelAdjusted = true;
                    notes.add("已切换为更省预算的酒店");
                    continue;
                }
            }
            if (recommends(reflection, "reduce_budget")) {
                appendDiagnosis(notes, evaluation.getBudgetDiagnosis());
                Attraction expensiveTarget = findMostExpensiveAttraction(workingAttractions);
                Attraction cheaperAlternative = selectReplacementAttraction(
                        allAttractions,
                        workingAttractions,
                        usedAttractionNames,
                        workingHotel,
                        request.getPreferences(),
                        constraints,
                        currentWeather,
                        dailyBudgetLimit,
                        expensiveTarget,
                        "budget"
                );
                if (expensiveTarget != null && cheaperAlternative != null) {
                    replaceAttraction(workingAttractions, expensiveTarget, cheaperAlternative);
                    notes.add("已将 " + expensiveTarget.getName() + " 调整为更省预算的 " + cheaperAlternative.getName());
                    continue;
                }
            }
            if (recommends(reflection, "compress_route")) {
                appendDiagnosis(notes, evaluation.getRouteDiagnosis());
                Attraction farthestTarget = findFarthestAttraction(workingAttractions, workingHotel);
                Attraction closerAlternative = selectReplacementAttraction(
                        allAttractions,
                        workingAttractions,
                        usedAttractionNames,
                        workingHotel,
                        request.getPreferences(),
                        constraints,
                        currentWeather,
                        dailyBudgetLimit,
                        farthestTarget,
                        "distance"
                );
                if (farthestTarget != null && closerAlternative != null) {
                    replaceAttraction(workingAttractions, farthestTarget, closerAlternative);
                    notes.add("已将 " + farthestTarget.getName() + " 调整为更近的 " + closerAlternative.getName());
                    continue;
                }
            }
            if (workingAttractions.size() > 1) {
                Attraction removed = selectAttractionToTrim(workingAttractions, workingHotel, evaluation.isRouteTooLong());
                workingAttractions.remove(removed);
                if (evaluation.isRouteTooLong()) {
                    notes.add("已减少景点 " + removed.getName() + " 以压缩通勤");
                } else {
                    notes.add("已减少景点 " + removed.getName() + " 以控制预算");
                }
                continue;
            }
            return new DailyPlanDecision(workingHotel, workingAttractions, transportation, meals, routePlan, estimatedCost, notes);
        }
    }

    private void enforceHardBudgetCuts(List<Attraction> workingAttractions,
                                       Hotel workingHotel,
                                       int dailyBudgetLimit,
                                       List<String> notes) {
        int maxAttractions = determineHardBudgetAttractionLimit(dailyBudgetLimit, workingHotel);
        while (workingAttractions.size() > maxAttractions) {
            Attraction removed = workingAttractions.remove(workingAttractions.size() - 1);
            notes.add("低预算模式已减少景点 " + removed.getName());
        }
    }

    private int determineHardBudgetAttractionLimit(int dailyBudgetLimit, Hotel hotel) {
        int hotelCost = estimateHotelCost(hotel);
        if (dailyBudgetLimit <= 380 || hotelCost >= dailyBudgetLimit * 0.65) {
            return 1;
        }
        if (dailyBudgetLimit <= 520 || hotelCost >= dailyBudgetLimit * 0.52) {
            return 2;
        }
        return 3;
    }

    private Hotel selectBudgetFriendlyHotel(List<Hotel> hotels,
                                            List<Attraction> attractions,
                                            int dailyBudgetLimit,
                                            Hotel currentHotel) {
        if (hotels == null || hotels.isEmpty()) {
            return currentHotel;
        }
        Hotel bestHotel = currentHotel;
        double bestScore = Double.MAX_VALUE;
        Location attractionCenter = calculateCenter(attractions);
        for (Hotel hotel : hotels) {
            if (currentHotel != null && hotel.getEstimatedCost() >= currentHotel.getEstimatedCost()) {
                continue;
            }
            double budgetPenalty = hotel.getEstimatedCost() > dailyBudgetLimit * 0.48 ? 10.0 : 0.0;
            double distancePenalty = attractionCenter == null || hotel.getLocation() == null
                    ? 0.0
                    : calculateDistanceKm(hotel.getLocation(), attractionCenter);
            double score = budgetPenalty + distancePenalty;
            if (score < bestScore) {
                bestScore = score;
                bestHotel = hotel;
            }
        }
        return bestHotel;
    }

    private Attraction findOutdoorAttractionForReplacement(List<Attraction> attractions) {
        for (int index = attractions.size() - 1; index >= 0; index--) {
            Attraction attraction = attractions.get(index);
            if (!isIndoorFriendly(attraction)) {
                return attraction;
            }
        }
        return null;
    }

    private Attraction findMostExpensiveAttraction(List<Attraction> attractions) {
        Attraction target = null;
        for (Attraction attraction : attractions) {
            if (target == null || attraction.getTicketPrice() > target.getTicketPrice()) {
                target = attraction;
            }
        }
        return target;
    }

    private Attraction findFarthestAttraction(List<Attraction> attractions, Hotel hotel) {
        if (hotel == null || hotel.getLocation() == null) {
            return attractions.get(attractions.size() - 1);
        }
        Attraction target = null;
        double farthestDistance = -1;
        for (Attraction attraction : attractions) {
            if (attraction.getLocation() == null) {
                continue;
            }
            double distance = calculateDistanceKm(hotel.getLocation(), attraction.getLocation());
            if (distance > farthestDistance) {
                farthestDistance = distance;
                target = attraction;
            }
        }
        return target == null ? attractions.get(attractions.size() - 1) : target;
    }

    private Attraction selectReplacementAttraction(List<Attraction> allAttractions,
                                                   List<Attraction> workingAttractions,
                                                   Set<String> usedAttractionNames,
                                                   Hotel hotel,
                                                   List<String> preferences,
                                                   PlanningConstraints constraints,
                                                   WeatherInfo currentWeather,
                                                   int dailyBudgetLimit,
                                                   Attraction target,
                                                   String mode) {
        if (target == null) {
            return null;
        }
        List<Attraction> nearbyAttractions = filterNearbyAttractions(allAttractions, hotel);
        //对nearbyAttractions做一个排序
        List<Attraction> candidateAttractions = prioritizeCandidateAttractions(
                nearbyAttractions.isEmpty() ? allAttractions : nearbyAttractions,
                preferences,
                constraints,
                currentWeather,
                dailyBudgetLimit
        );
        for (Attraction candidate : candidateAttractions) {
            if (candidate.getName().equals(target.getName())) {
                continue;
            }
            if (containsAttraction(workingAttractions, candidate.getName())) {
                continue;
            }
            if (usedAttractionNames.contains(candidate.getName())) {
                continue;
            }
            if (!isReasonablyCloseExcludingTarget(workingAttractions, target, candidate)) {
                continue;
            }
            if ("indoor".equals(mode) && !isIndoorFriendly(candidate)) {
                continue;
            }
            if ("budget".equals(mode) && candidate.getTicketPrice() >= target.getTicketPrice()) {
                continue;
            }
            if ("distance".equals(mode) && !isCloserToHotel(candidate, target, hotel)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private void replaceAttraction(List<Attraction> attractions, Attraction original, Attraction replacement) {
        for (int index = 0; index < attractions.size(); index++) {
            if (attractions.get(index).getName().equals(original.getName())) {
                attractions.set(index, replacement);
                return;
            }
        }
    }

    private boolean isReasonablyCloseExcludingTarget(List<Attraction> workingAttractions,
                                                     Attraction target,
                                                     Attraction candidate) {
        List<Attraction> remaining = new ArrayList<>();
        for (Attraction attraction : workingAttractions) {
            if (!attraction.getName().equals(target.getName())) {
                remaining.add(attraction);
            }
        }
        return isReasonablyClose(remaining, candidate);
    }

    private boolean isCloserToHotel(Attraction candidate, Attraction target, Hotel hotel) {
        if (hotel == null || hotel.getLocation() == null || candidate.getLocation() == null || target == null || target.getLocation() == null) {
            return true;
        }
        return calculateDistanceKm(hotel.getLocation(), candidate.getLocation())
                < calculateDistanceKm(hotel.getLocation(), target.getLocation());
    }

    private Attraction selectAttractionToTrim(List<Attraction> attractions, Hotel hotel, boolean routeTooLong) {
        if (routeTooLong) {
            return findFarthestAttraction(attractions, hotel);
        }
        return findMostExpensiveAttraction(attractions);
    }

    private void appendDiagnosis(List<String> notes, String diagnosis) {
        if (diagnosis == null || diagnosis.isBlank() || notes.contains(diagnosis)) {
            return;
        }
        notes.add(diagnosis);
    }

    private void appendReflectionNotes(List<String> notes, List<String> reflectionNotes) {
        if (reflectionNotes == null || reflectionNotes.isEmpty()) {
            return;
        }
        for (String reflectionNote : reflectionNotes) {
            appendDiagnosis(notes, reflectionNote);
        }
    }

    private boolean recommends(DailyPlanReflection reflection, String action) {
        return reflection != null && action.equals(reflection.getRecommendedAction());
    }
    //判断交通方式，如果前端有传就用前端的，如果没有就遍历景点的duringtime总和返回推荐交通方式
    private String decideTransportation(TripPlanRequest request,
                                        PlanningConstraints constraints,
                                        List<Attraction> dailyAttractions) {
        if (request.getTransportation() != null && !request.getTransportation().isBlank()) {
            return request.getTransportation();
        }
        int totalVisitDuration = dailyAttractions.stream().mapToInt(Attraction::getVisitDuration).sum();
        if (totalVisitDuration > 360) {
            return "公共交通 + 短途打车";
        }
        if (constraints != null && constraints.isRelaxedPace()) {
            return "公共交通 + 短途打车";
        }
        return "公共交通";
    }

    private List<Meal> buildMeals(String city,
                                  List<String> preferences,
                                  PlanningConstraints constraints,
                                  List<Attraction> dailyAttractions,
                                  int dailyBudgetLimit) {
        List<Meal> meals = new ArrayList<>();
        boolean premiumBudget = dailyBudgetLimit >= 1600;
        boolean luxuryBudget = dailyBudgetLimit >= 2500;
        boolean budgetTight = dailyBudgetLimit < 700;
        boolean budgetVeryTight = dailyBudgetLimit < 520;
        boolean budgetCritical = dailyBudgetLimit < 380;
        int breakfastCost = luxuryBudget ? 58 : (premiumBudget ? 42 : (budgetCritical ? 12 : (budgetVeryTight ? 15 : (budgetTight ? 20 : 30))));
        int lunchCost = luxuryBudget ? 168 : (premiumBudget ? 118 : (budgetCritical ? 25 : (budgetVeryTight ? 35 : (budgetTight ? 45 : 60))));
        int dinnerCost = luxuryBudget ? 238 : (premiumBudget ? 168 : (budgetCritical ? 35 : (budgetVeryTight ? 50 : (budgetTight ? 70 : 90))));
        meals.add(new Meal("breakfast", city + "风味早餐", dailyAttractions.get(0).getAddress(), dailyAttractions.get(0).getLocation(),
                budgetCritical ? "严格控预算，优先便利店早餐或社区早餐铺"
                        : (luxuryBudget ? "早餐升级为更完整的本地特色体验"
                        : (premiumBudget ? "早餐适度升级，兼顾舒适度和体验"
                        : (budgetTight ? "控制预算，选择高性价比早餐" : "轻量开启当天行程"))), breakfastCost));
        meals.add(new Meal("lunch",
                preferences.contains("美食") ? "本地特色午餐" : "简洁午餐",
                dailyAttractions.get(0).getAddress(),
                dailyAttractions.get(0).getLocation(),
                preferences.contains("美食") && luxuryBudget ? "午餐升级为高口碑本地特色餐厅"
                        : (preferences.contains("美食") && premiumBudget ? "优先安排口碑较好的特色餐厅"
                        : (preferences.contains("美食") && !budgetTight ? "优先选择口碑稳定的热门店铺"
                        : (budgetCritical ? "严格控预算，安排基础简餐" : "控制节奏和预算，安排稳定简餐"))),
                lunchCost));
        String dinnerDescription = constraints != null && constraints.isAvoidSeafood()
                ? "避开海鲜类餐厅，优先选择本地家常菜或小吃集合店"
                : "结合景点周边安排本地特色美食体验";
        if (budgetCritical) {
            dinnerDescription = "严格控预算，晚餐优先安排平价本地简餐";
        } else if (luxuryBudget) {
            dinnerDescription = constraints != null && constraints.isAvoidSeafood()
                    ? "避开海鲜前提下，晚餐升级为更完整的本地特色餐饮体验"
                    : "晚餐升级为更完整的城市代表性美食体验";
        } else if (premiumBudget) {
            dinnerDescription = constraints != null && constraints.isAvoidSeafood()
                    ? "避开海鲜前提下，晚餐优先安排更舒适的本地餐厅"
                    : "晚餐优先安排环境和口碑都更好的本地餐厅";
        }
        Attraction dinnerAttraction = dailyAttractions.get(dailyAttractions.size() - 1);
        meals.add(new Meal("dinner", "晚餐推荐", dinnerAttraction.getAddress(), dinnerAttraction.getLocation(), dinnerDescription, dinnerCost));
        return meals;
    }

    private Budget buildBudget(List<DayPlan> dayPlans, int requestedBudget) {
        int totalAttractions = 0;
        int totalHotels = 0;
        int totalMeals = 0;
        int totalTransportation = 0;
        for (DayPlan dayPlan : dayPlans) {
            totalAttractions += dayPlan.getAttractions().stream().mapToInt(Attraction::getTicketPrice).sum();
            totalMeals += dayPlan.getMeals().stream().mapToInt(Meal::getEstimatedCost).sum();
            totalTransportation += estimateTransportationCost(dayPlan.getRoutePlan());
            if (dayPlan.getHotel() != null) {
                totalHotels += dayPlan.getHotel().getEstimatedCost();
            }
        }
        int total = totalAttractions + totalHotels + totalMeals + totalTransportation;
        int remainingBudget = requestedBudget - total;
        boolean withinBudget = remainingBudget >= 0;
        String status = withinBudget ? "within_budget" : "over_budget";
        return new Budget(totalAttractions, totalHotels, totalMeals, totalTransportation, total,
                requestedBudget, remainingBudget, status, withinBudget);
    }

    private String buildOverallSuggestions(TripPlanRequest request,
                                           List<WeatherInfo> weatherInfo,
                                           List<String> planningNotes,
                                           List<DayPlan> dayPlans) {
        String weather = weatherInfo.isEmpty()
                ? "天气信息待补充"
                : weatherInfo.get(0).getDayWeather() + "，白天 " + weatherInfo.get(0).getDayTemp() + "°C";
        int routeMinutes = dayPlans.stream()
                .map(DayPlan::getRoutePlan)
                .filter(routePlan -> routePlan != null)
                .mapToInt(RoutePlan::getTotalDurationMinutes)
                .sum();
        StringBuilder builder = new StringBuilder(String.format(
                "%s 的 %d 日行程已生成。优先考虑%s，当前首日天气为%s。建议结合预算 %d 元和住宿偏好 %s 灵活调整节奏，预计总通勤约 %d 分钟。",
                request.getCity(),
                request.calculateDays(),
                joinPreferences(request.getPreferences()),
                weather,
                request.getBudget(),
                request.getAccommodation(),
                routeMinutes
        ));
        if (planningNotes != null && !planningNotes.isEmpty()) {
            builder.append(" 规划备注：").append(planningNotes.get(planningNotes.size() - 1));
        }
        return builder.toString();
    }

    private String joinPreferences(List<String> preferences) {
        StringJoiner joiner = new StringJoiner("、");
        for (String preference : preferences) {
            joiner.add(preference);
        }
        return joiner.toString();
    }

    private String joinAttractionNames(List<Attraction> attractions) {
        if (attractions == null || attractions.isEmpty()) {
            return "无";
        }
        StringJoiner joiner = new StringJoiner("、");
        for (Attraction attraction : attractions) {
            joiner.add(attraction.getName());
        }
        return joiner.toString();
    }

    private String safeHotelName(Hotel hotel) {
        return hotel == null ? "未选择酒店" : hotel.getName();
    }
    //根据景点中心位置和酒店价格获取besthotel
    private Hotel selectHotel(List<Hotel> hotels, List<Attraction> attractions, int dailyBudgetLimit) {
        if (hotels == null || hotels.isEmpty()) {
            return null;
        }
        Location attractionCenter = calculateCenter(attractions);
        Hotel bestHotel = hotels.get(0);
        double bestScore = Double.MAX_VALUE;

        for (Hotel hotel : hotels) {
            double budgetPenalty = resolveHotelBudgetPenalty(hotel, dailyBudgetLimit);
            double distancePenalty = attractionCenter == null || hotel.getLocation() == null
                    ? 0.0
                    : calculateDistanceKm(hotel.getLocation(), attractionCenter);
            double qualityAdjustment = resolveHotelQualityAdjustment(hotel, dailyBudgetLimit);
            double score = budgetPenalty + distancePenalty + qualityAdjustment;
            if (score < bestScore) {
                bestScore = score;
                bestHotel = hotel;
            }
        }
        return bestHotel;
    }

    private double resolveHotelBudgetPenalty(Hotel hotel, int dailyBudgetLimit) {
        int hotelCost = hotel.getEstimatedCost();
        if (dailyBudgetLimit <= 380) {
            return hotelCost > dailyBudgetLimit * 0.42 ? 50.0 : 0.0;
        }
        if (dailyBudgetLimit <= 520) {
            return hotelCost > dailyBudgetLimit * 0.48 ? 24.0 : 0.0;
        }
        if (dailyBudgetLimit <= 700) {
            return hotelCost > dailyBudgetLimit * 0.55 ? 12.0 : 0.0;
        }
        return hotelCost > dailyBudgetLimit * 0.55 ? 8.0 : 0.0;
    }

    private double resolveHotelQualityAdjustment(Hotel hotel, int dailyBudgetLimit) {
        if (hotel == null) {
            return 0.0;
        }
        double ratingScore = parseHotelRatingScore(hotel);
        if (dailyBudgetLimit >= 2500) {
            double costBonus = hotel.getEstimatedCost() <= dailyBudgetLimit * 0.7 ? hotel.getEstimatedCost() * -0.004 : 0.0;
            return costBonus - ratingScore * 6.0;
        }
        if (dailyBudgetLimit >= 1600) {
            double costBonus = hotel.getEstimatedCost() <= dailyBudgetLimit * 0.6 ? hotel.getEstimatedCost() * -0.002 : 0.0;
            return costBonus - ratingScore * 3.5;
        }
        return 0.0;
    }

    private double parseHotelRatingScore(Hotel hotel) {
        if (hotel.getRating() == null || hotel.getRating().isBlank()) {
            return 4.0;
        }
        try {
            return Double.parseDouble(hotel.getRating());
        } catch (NumberFormatException exception) {
            return 4.0;
        }
    }

    private int estimateDailyCost(List<Attraction> attractions, Hotel hotel, List<Meal> meals, RoutePlan routePlan) {
        int ticketCost = attractions.stream().mapToInt(Attraction::getTicketPrice).sum();
        int hotelCost = estimateHotelCost(hotel);
        int mealCost = meals.stream().mapToInt(Meal::getEstimatedCost).sum();
        int transportCost = estimateTransportationCost(routePlan);
        return ticketCost + hotelCost + mealCost + transportCost;
    }

    private int estimateTransportationCost(RoutePlan routePlan) {
        if (routePlan == null) {
            return 120;
        }
        double distance = routePlan.getTotalDistanceKm();
        if (routePlan.getTransportationMode() != null && routePlan.getTransportationMode().contains("步行")) {
            return 20;
        }
        if (routePlan.getTransportationMode() != null && routePlan.getTransportationMode().contains("打车")) {
            return Math.max(60, (int) Math.round(distance * 9));
        }
        return Math.max(35, (int) Math.round(distance * 4.5));
    }

    private int estimateHotelCost(Hotel hotel) {
        return hotel == null ? 0 : hotel.getEstimatedCost();
    }

    private boolean containsAttraction(List<Attraction> attractions, String attractionName) {
        for (Attraction attraction : attractions) {
            if (attraction.getName().equals(attractionName)) {
                return true;
            }
        }
        return false;
    }
    //做第一层过滤：先删掉明显不合适的景点,再把离大部队太远的景点剔掉
    private List<Attraction> sanitizeAttractions(List<Attraction> attractions) {
        List<Attraction> scenicAttractions = new ArrayList<>();
        for (Attraction attraction : attractions) {
            if (attraction.getLocation() == null) {
                continue;
            }
            if (isFoodLikeAttraction(attraction) || isInvalidAttraction(attraction)) {
                continue;
            }
            scenicAttractions.add(attraction);
        }
        if (scenicAttractions.size() <= 3) {
            return scenicAttractions.isEmpty() ? attractions : scenicAttractions;
        }

        Location center = calculateCenter(scenicAttractions);
        List<Attraction> clusteredAttractions = new ArrayList<>();
        for (Attraction attraction : scenicAttractions) {
            if (center == null || calculateDistanceKm(attraction.getLocation(), center) <= 35) {
                clusteredAttractions.add(attraction);
            }
        }
        return clusteredAttractions.size() >= 3 ? clusteredAttractions : scenicAttractions;
    }
    //遍历景点，过滤掉与酒店距离远的景点
    private List<Attraction> filterNearbyAttractions(List<Attraction> attractions, Hotel hotel) {
        if (hotel == null || hotel.getLocation() == null) {
            return attractions;
        }
        List<Attraction> nearbyAttractions = new ArrayList<>();
        for (Attraction attraction : attractions) {
            if (attraction.getLocation() == null) {
                continue;
            }
            if (calculateDistanceKm(hotel.getLocation(), attraction.getLocation()) <= 25) {
                nearbyAttractions.add(attraction);
            }
        }
        return nearbyAttractions.size() >= 2 ? nearbyAttractions : attractions;
    }

    private boolean isReasonablyClose(List<Attraction> selected, Attraction candidate) {
        if (selected.isEmpty() || candidate.getLocation() == null) {
            return true;
        }
        for (Attraction attraction : selected) {
            if (attraction.getLocation() != null
                    && calculateDistanceKm(attraction.getLocation(), candidate.getLocation()) > 30) {
                return false;
            }
        }
        return true;
    }

    private boolean isFoodLikeAttraction(Attraction attraction) {
        String category = attraction.getCategory() == null ? "" : attraction.getCategory();
        String description = attraction.getDescription() == null ? "" : attraction.getDescription();
        return category.contains("美食")
                || description.contains("餐饮服务")
                || description.contains("中餐厅")
                || description.contains("咖啡厅")
                || description.contains("火锅")
                || description.contains("烧烤");
    }

    private boolean isInvalidAttraction(Attraction attraction) {
        String name = attraction.getName() == null ? "" : attraction.getName();
        String description = attraction.getDescription() == null ? "" : attraction.getDescription();
        return description.contains("公司企业")
                || description.contains("金融保险服务")
                || description.contains("汽车服务")
                || description.contains("商务住宅")
                || description.contains("政府机构及社会团体")
                || name.contains("公司")
                || name.contains("有限公司");
    }

    private Location calculateCenter(List<Attraction> attractions) {
        double longitudeSum = 0;
        double latitudeSum = 0;
        int count = 0;
        for (Attraction attraction : attractions) {
            if (attraction.getLocation() == null) {
                continue;
            }
            longitudeSum += attraction.getLocation().getLongitude();
            latitudeSum += attraction.getLocation().getLatitude();
            count++;
        }
        if (count == 0) {
            return null;
        }
        return new Location(longitudeSum / count, latitudeSum / count);
    }
    //“根据两个经纬度，算两点直线球面距离（公里）的方法”
    private double calculateDistanceKm(Location first, Location second) {
        double latitudeDistance = Math.toRadians(second.getLatitude() - first.getLatitude());
        double longitudeDistance = Math.toRadians(second.getLongitude() - first.getLongitude());
        double startLatitude = Math.toRadians(first.getLatitude());
        double endLatitude = Math.toRadians(second.getLatitude());

        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private static class DailyPlanDecision {

        private final Hotel hotel;
        private final List<Attraction> attractions;
        private final String transportation;
        private final List<Meal> meals;
        private final RoutePlan routePlan;
        private final int estimatedCost;
        private final List<String> notes;

        private DailyPlanDecision(Hotel hotel,
                                  List<Attraction> attractions,
                                  String transportation,
                                  List<Meal> meals,
                                  RoutePlan routePlan,
                                  int estimatedCost,
                                  List<String> notes) {
            this.hotel = hotel;
            this.attractions = attractions;
            this.transportation = transportation;
            this.meals = meals;
            this.routePlan = routePlan;
            this.estimatedCost = estimatedCost;
            this.notes = notes;
        }
    }
}
