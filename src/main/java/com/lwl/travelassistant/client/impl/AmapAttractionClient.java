package com.lwl.travelassistant.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.AttractionClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.exception.TripPlanningException;
import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.AttractionQuery;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AmapAttractionClient implements AttractionClient {

    private static final Logger log = LoggerFactory.getLogger(AmapAttractionClient.class);

    private final TravelProviderProperties properties;
    private final InMemoryAttractionClient fallbackClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AmapAttractionClient(TravelProviderProperties properties,
                                ObjectMapper objectMapper,
                                InMemoryAttractionClient fallbackClient) {
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
    public List<Attraction> findAttractions(AttractionQuery query) {
        if (properties.getAmap().isMockResponse()) {
            return fallbackClient.findAttractions(query);
        }
        validateAmapConfiguration();
        List<Attraction> attractions = requestAttractions(query);
        if (attractions.isEmpty()) {
            log.warn("高德 POI 未返回有效景点，回退到本地景点库: city={}", query.getCity());
            return fallbackClient.findAttractions(query);
        }
        return attractions;
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                "amap-poi-search",
                "高德地图 POI 搜索",
                properties.getAmap().isMockResponse()
        );
    }

    private void validateAmapConfiguration() {
        if (properties.getAmap().getApiKey() == null || properties.getAmap().getApiKey().isBlank()) {
            throw new TripPlanningException("已选择高德景点 provider，但尚未配置 travel.providers.amap.api-key");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Attraction> requestAttractions(AttractionQuery query) {
        List<Map<String, Object>> pois = new ArrayList<>();
        List<String> keywords = buildKeywords(query);
        for (String keyword : keywords) {
            Map<String, Object> response = fetchPoiResponse(query.getCity(), keyword);
            List<Map<String, Object>> poiItems = (List<Map<String, Object>>) response.get("pois");
            if (poiItems == null) {
                continue;
            }
            for (Map<String, Object> poi : poiItems) {
                if (!containsPoiById(pois, String.valueOf(poi.get("id")))) {
                    pois.add(poi);
                }
            }
        }
        return pois.stream()
                .filter(this::isValidAttractionPoi)
                .map(this::toAttraction)
                .filter(attraction -> attraction.getLocation() != null)
                .toList();
    }
    private List<String> buildKeywords(AttractionQuery query) {
        List<String> keywords = query.getPreferences().stream()
                .map(this::mapPreferenceToKeyword)
                .distinct()
                .toList();

        if (keywords.isEmpty()) {
            return List.of("景点");
        }
        return keywords;
    }


    private String mapPreferenceToKeyword(String preference) {
        String normalized = preference == null ? "" : preference.toLowerCase(Locale.ROOT);
        if (normalized.contains("自然")) {
            return "景点 公园";
        }
        if (normalized.contains("历史") || normalized.contains("人文")) {
            return "博物馆 古迹";
        }
        if (normalized.contains("美食")) {
            return "美食街 步行街 夜市";
        }
        if (normalized.contains("休闲") || normalized.contains("轻松")) {
            return "公园 商圈";
        }
        if (normalized.contains("艺术")) {
            return "美术馆 艺术中心";
        }
        return "景点";
    }

    @SuppressWarnings("unchecked")//别对这个方法里的“未经检查的类型转换”报警告。
    private Map<String, Object> fetchPoiResponse(String city, String keyword) {
        List<URI> candidateUris = buildCandidateUris(city, keyword);//两个url（http，https）
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
                    throw new TripPlanningException("高德景点检索失败：" + info + "（infocode=" + infoCode + "）");
                }
                return responseBody;
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add(uri + " -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }
        //前面是java方式发送真实请求，如果失败就通过终端crul发送，握手问题很多就发生在java方式发送这里
        if (properties.getAmap().isCurlFallbackEnabled()) {
            log.warn("HTTP client 调用高德 POI 失败，开始尝试 curl fallback: {}", String.join(" | ", failures));
            try {
                return fetchPoiByCurl(candidateUris, failures);
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add("curl fallback -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        throw new TripPlanningException("高德景点检索请求异常：" + String.join(" | ", failures));
    }

    private Map<String, Object> fetchPoiByCurl(List<URI> candidateUris, List<String> failures) throws Exception {
        for (URI uri : candidateUris) {
            List<String> command = List.of(
                    "/usr/bin/curl",
                    "-s",
                    "--connect-timeout", "10",
                    "--max-time", "20",
                    uri.toString()
            );
            log.warn("执行 curl fallback 请求高德 POI: {}", uri);
            Process process = new ProcessBuilder(command).start();//把刚刚那堆命令参数交给系统
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
                throw new TripPlanningException("高德景点检索失败：" + info + "（infocode=" + infoCode + "）");
            }
            return responseBody;
        }
        throw new TripPlanningException("curl fallback 未获取到有效 POI 响应：" + String.join(" | ", failures));
    }

    private List<URI> buildCandidateUris(String city, String keyword) {
        List<URI> candidates = new ArrayList<>();
        String configuredBaseUrl = properties.getAmap().getBaseUrl();
        candidates.add(buildPoiUri(configuredBaseUrl, city, keyword));
        if (configuredBaseUrl.startsWith("https://")) {
            candidates.add(buildPoiUri(configuredBaseUrl.replace("https://", "http://"), city, keyword));
        } else if (configuredBaseUrl.startsWith("http://")) {
            candidates.add(buildPoiUri(configuredBaseUrl.replace("http://", "https://"), city, keyword));
        }
        return deduplicateUris(candidates);
    }

    private URI buildPoiUri(String baseUrl, String city, String keyword) {
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

    private Attraction toAttraction(Map<String, Object> poi) {
        String name = safeString(poi.get("name"), "未知景点");
        String address = buildAddress(poi);
        Location location = parseLocation(safeString(poi.get("location"), ""));
        String type = safeString(poi.get("type"), "城市景点");
        String category = mapTypeToCategory(type);
        return new Attraction(
                name,
                address,
                location,
                inferVisitDuration(category),
                buildDescription(name, type, address),
                category,
                parseRating(poi),
                parsePhotoUrl(poi),
                inferTicketPrice(category)
        );
    }

    private boolean isValidAttractionPoi(Map<String, Object> poi) {
        String type = safeString(poi.get("type"), "");
        String name = safeString(poi.get("name"), "");
        String normalizedType = type.toLowerCase(Locale.ROOT);
        String normalizedName = name.toLowerCase(Locale.ROOT);

        if (normalizedType.contains("公司企业")
                || normalizedType.contains("餐饮服务")
                || normalizedType.contains("金融保险服务")
                || normalizedType.contains("汽车服务")
                || normalizedType.contains("汽车维修")
                || normalizedType.contains("汽车销售")
                || normalizedType.contains("房地产")
                || normalizedType.contains("商务住宅")
                || normalizedType.contains("医疗保健服务")
                || normalizedType.contains("政府机构及社会团体")) {
            return false;
        }

        return !normalizedName.contains("有限公司")
                && !normalizedName.contains("公司")
                && !normalizedName.contains("餐饮")
                && !normalizedName.contains("奶茶")
                && !normalizedName.contains("火锅")
                && !normalizedName.contains("烧烤");
    }

    @SuppressWarnings("unchecked")
    private String parsePhotoUrl(Map<String, Object> poi) {
        Object photosObject = poi.get("photos");
        if (!(photosObject instanceof List<?> photos) || photos.isEmpty()) {
            return null;
        }
        for (Object photoObject : photos) {
            if (photoObject instanceof Map<?, ?> photo) {
                Object url = photo.get("url");
                if (url != null && !String.valueOf(url).isBlank()) {
                    return String.valueOf(url);
                }
            }
        }
        return null;
    }

    private boolean containsPoiById(List<Map<String, Object>> pois, String poiId) {
        for (Map<String, Object> poi : pois) {
            if (String.valueOf(poi.get("id")).equals(poiId)) {
                return true;
            }
        }
        return false;
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

    private String mapTypeToCategory(String type) {
        String normalized = type.toLowerCase(Locale.ROOT);
        if (normalized.contains("风景") || normalized.contains("公园") || normalized.contains("景点")) {
            return "自然风景";
        }
        if (normalized.contains("博物馆") || normalized.contains("古迹") || normalized.contains("寺庙") || normalized.contains("名胜")) {
            return "人文历史";
        }
        if (normalized.contains("餐饮") || normalized.contains("购物") || normalized.contains("商场")) {
            return "美食购物";
        }
        return "城市漫游";
    }

    private int inferVisitDuration(String category) {
        if ("自然风景".equals(category)) {
            return 180;
        }
        if ("人文历史".equals(category)) {
            return 150;
        }
        if ("美食购物".equals(category)) {
            return 90;
        }
        return 120;
    }

    private int inferTicketPrice(String category) {
        if ("自然风景".equals(category)) {
            return 60;
        }
        if ("人文历史".equals(category)) {
            return 50;
        }
        if ("美食购物".equals(category)) {
            return 0;
        }
        return 30;
    }

    private String buildDescription(String name, String type, String address) {
        return String.format("%s，适合安排在%s周边游览，POI 类型为 %s。", name, address, type);
    }

    private Double parseRating(Map<String, Object> poi) {
        Object bizExtObject = poi.get("biz_ext");
        if (bizExtObject instanceof Map<?, ?> bizExtMap) {
            Object rating = bizExtMap.get("rating");
            if (rating != null && !String.valueOf(rating).isBlank()) {
                try {
                    return Double.parseDouble(String.valueOf(rating));
                } catch (NumberFormatException ignored) {
                    return 4.5;
                }
            }
        }
        return 4.5;
    }

    private String safeString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() || "[]".equals(text) ? defaultValue : text;
    }
}
