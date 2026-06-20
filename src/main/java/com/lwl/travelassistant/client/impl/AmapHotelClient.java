package com.lwl.travelassistant.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.HotelClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.exception.TripPlanningException;
import com.lwl.travelassistant.model.Hotel;
import com.lwl.travelassistant.model.HotelQuery;
import com.lwl.travelassistant.model.Location;
import com.lwl.travelassistant.model.ProviderMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AmapHotelClient implements HotelClient {

    private static final Logger log = LoggerFactory.getLogger(AmapHotelClient.class);

    private final TravelProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final RuleBasedHotelClient fallbackClient;
    private final HttpClient httpClient;

    public AmapHotelClient(TravelProviderProperties properties,
                           ObjectMapper objectMapper,
                           RuleBasedHotelClient fallbackClient) {
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
    public List<Hotel> recommendHotels(HotelQuery query) {
        if (properties.getAmap().isMockResponse()) {
            return fallbackClient.recommendHotels(query);
        }
        validateAmapConfiguration();

        try {
            List<Hotel> hotels = requestHotels(query);
            if (hotels.isEmpty()) {
                log.warn("高德酒店检索未返回有效结果，回退到规则酒店: city={}", query.getCity());
                return fallbackClient.recommendHotels(query);
            }
            return hotels;
        } catch (Exception exception) {
            log.warn("高德酒店检索失败，回退到规则酒店: city={}, reason={}", query.getCity(), exception.getMessage());
            return fallbackClient.recommendHotels(query);
        }
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                "amap-hotel-search",
                "高德地图酒店检索",
                properties.getAmap().isMockResponse()
        );
    }

    private void validateAmapConfiguration() {
        if (properties.getAmap().getApiKey() == null || properties.getAmap().getApiKey().isBlank()) {
            throw new TripPlanningException("已选择高德酒店 provider，但尚未配置 travel.providers.amap.api-key");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Hotel> requestHotels(HotelQuery query) throws Exception {
        Map<String, Object> response = fetchHotelResponse(query);
        List<Map<String, Object>> pois = (List<Map<String, Object>>) response.get("pois");
        if (pois == null) {
            return List.of();
        }
        return pois.stream()
                .filter(poi -> isHotelInRequestedCity(poi, query.getCity()))
                .map(poi -> toHotel(poi, query))
                .filter(hotel -> hotel.getLocation() != null)
                .sorted((first, second) -> Integer.compare(first.getEstimatedCost(), second.getEstimatedCost()))
                .toList();
    }

    private boolean isHotelInRequestedCity(Map<String, Object> poi, String requestedCity) {
        String normalizedRequestedCity = normalizeCityName(requestedCity);
        if (normalizedRequestedCity.isBlank()) {
            return true;
        }
        String pname = normalizeCityName(safeString(poi.get("pname"), ""));
        String cityname = normalizeCityName(safeString(poi.get("cityname"), ""));
        String adname = normalizeCityName(safeString(poi.get("adname"), ""));
        String address = normalizeCityName(safeString(poi.get("address"), ""));
        return pname.contains(normalizedRequestedCity)
                || cityname.contains(normalizedRequestedCity)
                || adname.contains(normalizedRequestedCity)
                || address.contains(normalizedRequestedCity);
    }

    private String normalizeCityName(String city) {
        if (city == null) {
            return "";
        }
        return city.replace("省", "")
                .replace("市", "")
                .replace("壮族自治区", "")
                .replace("回族自治区", "")
                .replace("维吾尔自治区", "")
                .replace("自治区", "")
                .replace("特别行政区", "")
                .trim();
    }

    private Map<String, Object> fetchHotelResponse(HotelQuery query) throws Exception {
        List<URI> candidateUris = buildCandidateUris(query);
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
                    throw new TripPlanningException("高德酒店检索失败：" + info + "（infocode=" + infoCode + "）");
                }
                return responseBody;
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add(uri + " -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        if (properties.getAmap().isCurlFallbackEnabled()) {
            log.warn("HTTP client 调用高德酒店检索失败，开始尝试 curl fallback: {}", String.join(" | ", failures));
            return fetchHotelByCurl(candidateUris, failures);
        }

        throw new TripPlanningException("高德酒店检索请求异常：" + String.join(" | ", failures));
    }

    private Map<String, Object> fetchHotelByCurl(List<URI> candidateUris, List<String> failures) throws Exception {
        for (URI uri : candidateUris) {
            List<String> command = List.of(
                    "/usr/bin/curl",
                    "-s",
                    "--connect-timeout", "10",
                    "--max-time", "20",
                    uri.toString()
            );
            log.warn("执行 curl fallback 请求高德酒店检索: {}", uri);
            Process process = new ProcessBuilder(command).start();
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
                throw new TripPlanningException("高德酒店检索失败：" + info + "（infocode=" + infoCode + "）");
            }
            return responseBody;
        }
        throw new TripPlanningException("高德酒店检索 curl fallback 未获取到有效响应：" + String.join(" | ", failures));
    }

    private List<URI> buildCandidateUris(HotelQuery query) {
        List<URI> candidates = new ArrayList<>();
        String configuredBaseUrl = properties.getAmap().getBaseUrl();
        String keyword = buildHotelKeyword(query);
        candidates.add(buildHotelUri(configuredBaseUrl, query.getCity(), keyword));
        if (configuredBaseUrl.startsWith("https://")) {
            candidates.add(buildHotelUri(configuredBaseUrl.replace("https://", "http://"), query.getCity(), keyword));
        } else if (configuredBaseUrl.startsWith("http://")) {
            candidates.add(buildHotelUri(configuredBaseUrl.replace("http://", "https://"), query.getCity(), keyword));
        }
        return deduplicateUris(candidates);
    }

    private URI buildHotelUri(String baseUrl, String city, String keyword) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v3/place/text")
                .queryParam("key", properties.getAmap().getApiKey())
                .queryParam("keywords", keyword)
                .queryParam("city", city)
                .queryParam("citylimit", properties.getAmap().isCityLimit())
                .queryParam("offset", properties.getAmap().getPoiPageSize())
                .queryParam("page", 1)
                .queryParam("extensions", "all")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    private String buildHotelKeyword(HotelQuery query) {
        String accommodation = query.getAccommodation();
        String normalized = accommodation == null ? "" : accommodation.toLowerCase(Locale.ROOT);
        int budgetPerDay = Math.max(1, query.getBudget() / Math.max(1, query.getDays()));
        if (budgetPerDay <= 450) {
            return "经济型酒店 连锁酒店";
        }
        if (budgetPerDay <= 700 && (normalized.contains("豪华") || normalized.contains("高端") || normalized.contains("轻奢"))) {
            return "舒适型酒店 精品酒店";
        }
        if (normalized.contains("经济") || normalized.contains("便捷")) {
            return "经济型酒店 连锁酒店";
        }
        if (normalized.contains("舒适") || normalized.contains("精选")) {
            return "舒适型酒店 精品酒店";
        }
        if (normalized.contains("豪华") || normalized.contains("高端") || normalized.contains("轻奢")) {
            return "豪华酒店 高端酒店";
        }
        if (normalized.contains("民宿") || normalized.contains("客栈")) {
            return "民宿 客栈";
        }
        return "酒店";
    }

    @SuppressWarnings("unchecked")
    private Hotel toHotel(Map<String, Object> poi, HotelQuery query) {
        String name = safeString(poi.get("name"), "未知酒店");
        String address = buildAddress(poi);
        Location location = parseLocation(safeString(poi.get("location"), ""));
        String rating = extractRating(poi);
        int estimatedCost = extractEstimatedCost(poi, query);
        String priceRange = buildPriceRange(estimatedCost);
        String distance = buildDistanceLabel(poi);

        return new Hotel(
                name,
                address,
                location,
                priceRange,
                rating,
                distance,
                query.getAccommodation(),
                estimatedCost
        );
    }

    private String extractRating(Map<String, Object> poi) {
        Object bizExtValue = poi.get("biz_ext");
        if (bizExtValue instanceof Map<?, ?> bizExt) {
            String rating = safeString(bizExt.get("rating"), "");
            if (!rating.isBlank()) {
                return rating;
            }
        }
        return "4.3";
    }

    private int extractEstimatedCost(Map<String, Object> poi, HotelQuery query) {
        Object bizExtValue = poi.get("biz_ext");
        if (bizExtValue instanceof Map<?, ?> bizExt) {
            String cost = safeString(bizExt.get("cost"), "");
            if (!cost.isBlank()) {
                try {
                    return normalizeHotelCost((int) Math.round(Double.parseDouble(cost)), query);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return inferEstimatedCost(query);
    }

    private int inferEstimatedCost(HotelQuery query) {
        int budgetPerDay = Math.max(1, query.getBudget() / Math.max(1, query.getDays()));
        String accommodation = query.getAccommodation() == null ? "" : query.getAccommodation();
        if (budgetPerDay <= 450) {
            return Math.min(320, Math.max(220, budgetPerDay - 20));
        }
        if (accommodation.contains("经济")) {
            return Math.min(380, budgetPerDay);
        }
        if (accommodation.contains("舒适")) {
            return Math.min(680, Math.max(320, budgetPerDay));
        }
        if (accommodation.contains("豪华") || accommodation.contains("高端")) {
            return Math.min(1280, Math.max(520, budgetPerDay));
        }
        return Math.min(580, Math.max(360, budgetPerDay));
    }

    private int normalizeHotelCost(int rawCost, HotelQuery query) {
        int inferredCost = inferEstimatedCost(query);
        int budgetPerDay = Math.max(1, query.getBudget() / Math.max(1, query.getDays()));
        if (budgetPerDay <= 450) {
            return Math.min(rawCost, inferredCost);
        }
        return Math.max(220, Math.min(rawCost, Math.max(inferredCost, budgetPerDay + 120)));
    }

    private String buildPriceRange(int estimatedCost) {
        int low = Math.max(estimatedCost - 120, 0);
        int high = estimatedCost + 180;
        return low + "-" + high + "元";
    }

    private String buildDistanceLabel(Map<String, Object> poi) {
        String adname = safeString(poi.get("adname"), "");
        if (!adname.isBlank()) {
            return "位于" + adname + "区域";
        }
        return "位于城市核心片区";
    }

    private String buildAddress(Map<String, Object> poi) {
        String pname = safeString(poi.get("pname"), "");
        String cityname = safeString(poi.get("cityname"), "");
        String adname = safeString(poi.get("adname"), "");
        String address = safeString(poi.get("address"), "");
        String joined = String.join("", List.of(pname, cityname, adname, address));
        return joined.isBlank() ? "暂无详细地址" : joined;
    }

    private Location parseLocation(String rawLocation) {
        if (rawLocation == null || rawLocation.isBlank() || !rawLocation.contains(",")) {
            return null;
        }
        String[] parts = rawLocation.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Location(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

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

    private String safeString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? defaultValue : text;
    }
}
