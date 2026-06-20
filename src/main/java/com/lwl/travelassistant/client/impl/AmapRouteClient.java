package com.lwl.travelassistant.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.RouteClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.exception.TripPlanningException;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.MapPoint;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.RoutePlan;
import com.lwl.travelassistant.model.RouteQuery;
import com.lwl.travelassistant.model.RouteStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AmapRouteClient implements RouteClient {

    private static final Logger log = LoggerFactory.getLogger(AmapRouteClient.class);

    private final TravelProviderProperties properties;
    private final RuleBasedRouteClient fallbackClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AmapRouteClient(TravelProviderProperties properties,
                           ObjectMapper objectMapper,
                           RuleBasedRouteClient fallbackClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallbackClient = fallbackClient;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public RoutePlan planRoute(RouteQuery query) {
        if (properties.getAmap().isMockResponse()) {
            RoutePlan routePlan = fallbackClient.planRoute(query);
            routePlan.setProvider(getMetadata());
            routePlan.setSummary(routePlan.getSummary() + " 当前为高德路线 provider 的模拟返回。");
            return routePlan;
        }
        validateAmapConfiguration();
        try {
            return requestRoutePlan(query);
        } catch (TripPlanningException exception) {
            if (shouldFallbackToRuleRoute(exception)) {
                log.warn("高德路线触发限流或配额保护，回退到规则路线: {}", exception.getMessage());
                RoutePlan routePlan = fallbackClient.planRoute(query);
                routePlan.setSummary(routePlan.getSummary() + " 高德路线当前受限，已自动切换为规则路线估算。");
                return routePlan;
            }
            throw exception;
        }
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                "amap-route-service",
                "高德地图路线规划",
                properties.getAmap().isMockResponse()
        );
    }

    private void validateAmapConfiguration() {
        if (properties.getAmap().getApiKey() == null || properties.getAmap().getApiKey().isBlank()) {
            throw new TripPlanningException("已选择高德路线 provider，但尚未配置 travel.providers.amap.api-key");
        }
    }

    private RoutePlan requestRoutePlan(RouteQuery query) {
        //points相当于是构建了酒店-景点-酒店这条链路
        List<MapPoint> points = buildRoutePoints(query);
        if (points.size() < 2) {
            throw new TripPlanningException("高德路线规划至少需要两个有效坐标点");
        }

        List<RouteStep> steps = new ArrayList<>();
        double totalDistanceKm = 0;
        int totalDurationMinutes = 0;

        for (int index = 0; index < points.size() - 1; index++) {
            MapPoint fromPoint = points.get(index);
            MapPoint toPoint = points.get(index + 1);
            //发送请求获响应
            Map<String, Object> routeResponse = fetchSegmentRoute(query.getTransportation(), fromPoint, toPoint);
            Map<String, Object> summary = extractRouteSummary(routeResponse, query.getTransportation());
            double distanceKm = ((Number) summary.get("distanceKm")).doubleValue();
            int durationMinutes = ((Number) summary.get("durationMinutes")).intValue();
            totalDistanceKm += distanceKm;
            totalDurationMinutes += durationMinutes;
            steps.add(new RouteStep(
                    index + 1,
                    fromPoint.getName(),
                    toPoint.getName(),
                    query.getTransportation(),
                    round(distanceKm),
                    durationMinutes,
                    (String) summary.get("instruction")
            ));
        }

        return new RoutePlan(
                query.getCity(),
                query.getDayIndex(),
                points.get(0),
                points.get(points.size() - 1),
                steps,
                round(totalDistanceKm),
                totalDurationMinutes,
                query.getTransportation(),
                String.format("高德路线已生成，共 %d 段，预计 %.1f 公里，通勤约 %d 分钟。", steps.size(), round(totalDistanceKm), totalDurationMinutes),
                getMetadata()
        );
    }
    //这里是去构建线路，最后去if判断是吧hotel作为终点
    private List<MapPoint> buildRoutePoints(RouteQuery query) {
        List<MapPoint> points = new ArrayList<>();
        Hotel hotel = query.getHotel();
        if (hotel != null && hotel.getLocation() != null) {
            points.add(new MapPoint(hotel.getName(), hotel.getAddress(), hotel.getLocation(), "hotel"));
        }
        for (Attraction attraction : query.getAttractions()) {
            if (attraction.getLocation() != null) {
                points.add(new MapPoint(attraction.getName(), attraction.getAddress(), attraction.getLocation(), "attraction"));
            }
        }
        if (hotel != null && hotel.getLocation() != null && !query.getAttractions().isEmpty()) {
            points.add(new MapPoint(hotel.getName(), hotel.getAddress(), hotel.getLocation(), "hotel"));
        }
        return points;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRouteSummary(Map<String, Object> response, String transportation) {
        Map<String, Object> route = (Map<String, Object>) response.get("route");
        List<Map<String, Object>> paths = route == null ? List.of() : (List<Map<String, Object>>) route.get("paths");
        if (paths == null || paths.isEmpty()) {
            throw new TripPlanningException("高德路线规划返回为空，请稍后重试");
        }
        Map<String, Object> firstPath = paths.get(0);
        //起点和终点的步行距离
        double distanceMeters = parseDouble(firstPath.get("distance"));
        //步行预计时间
        double durationSeconds = parseDouble(firstPath.get("duration"));
        String instruction = extractInstruction(firstPath, transportation);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("distanceKm", distanceMeters / 1000.0);
        //Math.round(...) 意思是：四舍五入
        summary.put("durationMinutes", Math.max(1, (int) Math.round(durationSeconds / 60.0)));
        summary.put("instruction", instruction);
        return summary;
    }

    //返回步行结果列表，选择第一个
    @SuppressWarnings("unchecked")
    private String extractInstruction(Map<String, Object> firstPath, String transportation) {
        List<Map<String, Object>> steps = (List<Map<String, Object>>) firstPath.get("steps");
        if (steps != null && !steps.isEmpty() && steps.get(0).get("instruction") != null) {
            return String.valueOf(steps.get(0).get("instruction"));
        }
        return transportation + " 出行";
    }

    private Map<String, Object> fetchSegmentRoute(String transportation, MapPoint fromPoint, MapPoint toPoint) {
        List<URI> candidateUris = buildCandidateUris(transportation, fromPoint.getLocation(), toPoint.getLocation());
        List<String> failures = new ArrayList<>();

        for (URI uri : candidateUris) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/json")
                        .header("User-Agent", "travel-assistant-java/1.0")
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 500) {
                    failures.add(uri + " -> HTTP " + response.statusCode());
                    continue;
                }
                if (response.body() == null || response.body().isBlank()) {
                    failures.add(uri + " -> empty body");
                    continue;
                }
                Map<String, Object> responseBody = objectMapper.readValue(
                        response.body(),
                        new TypeReference<Map<String, Object>>() {}
                );
                if (!"1".equals(String.valueOf(responseBody.get("status")))) {
                    String info = String.valueOf(responseBody.getOrDefault("info", "未知错误"));
                    String infoCode = String.valueOf(responseBody.getOrDefault("infocode", "unknown"));
                    throw new TripPlanningException("高德路线规划调用失败：" + info + "（infocode=" + infoCode + "）");
                }
                return responseBody;
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add(uri + " -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        if (properties.getAmap().isCurlFallbackEnabled()) {
            log.warn("HTTP client 调用高德失败，开始尝试 curl fallback: {}", String.join(" | ", failures));
            try {
                return fetchRouteByCurl(candidateUris, failures);
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add("curl fallback -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        throw new TripPlanningException("高德路线规划请求异常：" + String.join(" | ", failures));
    }
    //通过curl终端发送请求
    private Map<String, Object> fetchRouteByCurl(List<URI> candidateUris, List<String> failures) throws Exception {
        for (URI uri : candidateUris) {
            List<String> command = List.of(
                    "/usr/bin/curl",
                    "-s",
                    "--connect-timeout", "10",
                    "--max-time", "20",
                    uri.toString()
            );
            log.warn("执行 curl fallback 请求高德: {}", uri);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            String body = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                failures.add("curl " + uri + " -> exitCode=" + exitCode + (error.isBlank() ? "" : ", stderr=" + error.trim()));
                continue;
            }
            if (body == null || body.isBlank()) {
                failures.add("curl " + uri + " -> empty body");
                continue;
            }

            Map<String, Object> responseBody = objectMapper.readValue(
                    body,
                    new TypeReference<Map<String, Object>>() {}
            );
            if (!"1".equals(String.valueOf(responseBody.get("status")))) {
                String info = String.valueOf(responseBody.getOrDefault("info", "未知错误"));
                String infoCode = String.valueOf(responseBody.getOrDefault("infocode", "unknown"));
                throw new TripPlanningException("高德路线规划调用失败：" + info + "（infocode=" + infoCode + "）");
            }
            return responseBody;
        }
        throw new TripPlanningException("curl fallback 未获取到有效响应：" + String.join(" | ", failures));
    }
    //构建url，分别以http和https
    private List<URI> buildCandidateUris(String transportation, Location origin, Location destination) {
        List<URI> candidates = new ArrayList<>();
        String configuredBaseUrl = properties.getAmap().getBaseUrl();
        candidates.add(buildRouteUri(configuredBaseUrl, transportation, origin, destination));
        if (configuredBaseUrl.startsWith("https://")) {
            candidates.add(buildRouteUri(configuredBaseUrl.replace("https://", "http://"), transportation, origin, destination));
        } else if (configuredBaseUrl.startsWith("http://")) {
            candidates.add(buildRouteUri(configuredBaseUrl.replace("http://", "https://"), transportation, origin, destination));
        }
        return deduplicateUris(candidates);
    }
    //去重
    private List<URI> deduplicateUris(List<URI> candidates) {
        List<URI> unique = new ArrayList<>();
        for (URI candidate : candidates) {
            boolean exists = false;
            for (URI existing : unique) {
                if (existing.toString().equals(candidate.toString())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                unique.add(candidate);
            }
        }
        return unique;
    }

    private URI buildRouteUri(String baseUrl, String transportation, Location origin, Location destination) {
        String path = isWalking(transportation) ? "/v3/direction/walking" : "/v3/direction/driving";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("key", properties.getAmap().getApiKey())
                .queryParam("origin", formatLocation(origin))
                .queryParam("destination", formatLocation(destination));
        if (!isWalking(transportation)) {
            builder.queryParam("strategy", properties.getAmap().getRouteStrategy())
                    .queryParam("extensions", properties.getAmap().getRouteExtensions());
        }
        return builder.encode(StandardCharsets.UTF_8).build().toUri();
    }

    private boolean isWalking(String transportation) {
        return transportation != null && transportation.contains("步行");
    }

    private boolean shouldFallbackToRuleRoute(TripPlanningException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("infocode=10021")
                || message.contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")
                || message.contains("USER_DAILY_QUERY_OVER_LIMIT")
                || message.contains("ACCESS_TOO_FREQUENT");
    }

    private String formatLocation(Location location) {
        if (location == null) {
            throw new TripPlanningException("路线规划缺少坐标信息");
        }
        return String.format("%.6f,%.6f", location.getLongitude(), location.getLatitude());
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
