package com.lwl.travelassistant.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwl.travelassistant.client.WeatherClient;
import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.exception.TripPlanningException;
import com.lwl.travelassistant.model.ProviderMetadata;
import com.lwl.travelassistant.model.WeatherInfo;
import com.lwl.travelassistant.model.WeatherQuery;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AmapWeatherClient implements WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(AmapWeatherClient.class);

    private final TravelProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final RuleBasedWeatherClient fallbackClient;
    private final HttpClient httpClient;

    public AmapWeatherClient(TravelProviderProperties properties,
                             ObjectMapper objectMapper,
                             RuleBasedWeatherClient fallbackClient) {
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
    public List<WeatherInfo> getWeather(WeatherQuery query) {
        if (properties.getAmap().isMockResponse()) {
            return fallbackClient.getWeather(query);
        }
        validateAmapConfiguration();

        List<WeatherInfo> fallbackWeather = fallbackClient.getWeather(query);
        try {
            String adcode = resolveAdcode(query.getCity());
            Map<String, Object> response = fetchWeatherResponse(adcode);
            Map<String, WeatherInfo> realWeatherByDate = extractWeatherByDate(response);
            if (realWeatherByDate.isEmpty()) {
                log.warn("高德天气未返回有效预报，回退到规则天气: city={}", query.getCity());
                return fallbackWeather;
            }
            return mergeWeather(fallbackWeather, realWeatherByDate);
        } catch (Exception exception) {
            log.warn("高德天气请求失败，回退到规则天气: city={}, reason={}", query.getCity(), exception.getMessage());
            return fallbackWeather;
        }
    }

    @Override
    public ProviderMetadata getMetadata() {
        return new ProviderMetadata(
                "amap-weather-service",
                "高德地图天气服务",
                properties.getAmap().isMockResponse()
        );
    }

    private void validateAmapConfiguration() {
        if (properties.getAmap().getApiKey() == null || properties.getAmap().getApiKey().isBlank()) {
            throw new TripPlanningException("已选择高德天气 provider，但尚未配置 travel.providers.amap.api-key");
        }
    }

    private List<WeatherInfo> mergeWeather(List<WeatherInfo> fallbackWeather, Map<String, WeatherInfo> realWeatherByDate) {
        List<WeatherInfo> merged = new ArrayList<>();
        for (WeatherInfo fallbackItem : fallbackWeather) {
            WeatherInfo realItem = realWeatherByDate.get(fallbackItem.getDate());
            if (realItem != null) {
                merged.add(realItem);
            } else {
                fallbackItem.setSource("rule-fallback");
                fallbackItem.setSourceNote("高德未返回该日期天气，已使用规则天气补全");
                merged.add(fallbackItem);
            }
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, WeatherInfo> extractWeatherByDate(Map<String, Object> response) {
        Map<String, WeatherInfo> realWeatherByDate = new LinkedHashMap<>();
        List<Map<String, Object>> forecasts = (List<Map<String, Object>>) response.get("forecasts");
        if (forecasts == null || forecasts.isEmpty()) {
            return realWeatherByDate;
        }

        Map<String, Object> firstForecast = forecasts.get(0);
        List<Map<String, Object>> casts = (List<Map<String, Object>>) firstForecast.get("casts");
        if (casts == null) {
            return realWeatherByDate;
        }

        for (Map<String, Object> cast : casts) {
            String date = safeString(cast.get("date"), null);
            if (date == null || date.isBlank()) {
                continue;
            }
            realWeatherByDate.put(date, new WeatherInfo(
                    date,
                    safeString(cast.get("dayweather"), "未知"),
                    safeString(cast.get("nightweather"), "未知"),
                    parseInt(cast.get("daytemp")),
                    parseInt(cast.get("nighttemp")),
                    buildWindDirection(cast),
                    buildWindPower(cast),
                    "amap-live",
                    "高德实时天气预报"
            ));
        }
        return realWeatherByDate;
    }

    private String buildWindDirection(Map<String, Object> cast) {
        String dayWind = safeString(cast.get("daywind"), "");
        String nightWind = safeString(cast.get("nightwind"), "");
        if (!dayWind.isBlank() && !nightWind.isBlank() && !dayWind.equals(nightWind)) {
            return dayWind + "转" + nightWind;
        }
        if (!dayWind.isBlank()) {
            return dayWind;
        }
        if (!nightWind.isBlank()) {
            return nightWind;
        }
        return "未知风向";
    }

    private String buildWindPower(Map<String, Object> cast) {
        String dayPower = safeString(cast.get("daypower"), "");
        String nightPower = safeString(cast.get("nightpower"), "");
        if (!dayPower.isBlank() && !nightPower.isBlank() && !dayPower.equals(nightPower)) {
            return dayPower + "-" + nightPower + "级";
        }
        if (!dayPower.isBlank()) {
            return dayPower + "级";
        }
        if (!nightPower.isBlank()) {
            return nightPower + "级";
        }
        return "未知风力";
    }

    private String resolveAdcode(String city) throws Exception {
        Map<String, Object> response = fetchJson(buildGeocodeCandidateUris(city), "高德城市编码解析");
        return extractAdcode(response, city);
    }

    @SuppressWarnings("unchecked")
    private String extractAdcode(Map<String, Object> response, String city) {
        List<Map<String, Object>> geocodes = (List<Map<String, Object>>) response.get("geocodes");
        if (geocodes == null || geocodes.isEmpty()) {
            throw new TripPlanningException("高德未返回城市编码：" + city);
        }
        String adcode = safeString(geocodes.get(0).get("adcode"), "");
        if (adcode.isBlank()) {
            throw new TripPlanningException("高德返回的城市编码为空：" + city);
        }
        return adcode;
    }

    private Map<String, Object> fetchWeatherResponse(String adcode) throws Exception {
        return fetchJson(buildWeatherCandidateUris(adcode), "高德天气查询");
    }

    private Map<String, Object> fetchJson(List<URI> candidateUris, String actionName) throws Exception {
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
                    throw new TripPlanningException(actionName + "失败：" + info + "（infocode=" + infoCode + "）");
                }
                return responseBody;
            } catch (TripPlanningException exception) {
                throw exception;
            } catch (Exception exception) {
                failures.add(uri + " -> " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        if (properties.getAmap().isCurlFallbackEnabled()) {
            log.warn("HTTP client 调用{}失败，开始尝试 curl fallback: {}", actionName, String.join(" | ", failures));
            return fetchJsonByCurl(candidateUris, actionName, failures);
        }

        throw new TripPlanningException(actionName + "请求异常：" + String.join(" | ", failures));
    }

    private Map<String, Object> fetchJsonByCurl(List<URI> candidateUris,
                                                String actionName,
                                                List<String> failures) throws Exception {
        for (URI uri : candidateUris) {
            List<String> command = List.of(
                    "/usr/bin/curl",
                    "-s",
                    "--connect-timeout", "10",
                    "--max-time", "20",
                    uri.toString()
            );
            log.warn("执行 curl fallback 请求{}: {}", actionName, uri);
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
                throw new TripPlanningException(actionName + "失败：" + info + "（infocode=" + infoCode + "）");
            }
            return responseBody;
        }
        throw new TripPlanningException(actionName + " curl fallback 未获取到有效响应：" + String.join(" | ", failures));
    }

    private List<URI> buildGeocodeCandidateUris(String city) {
        List<URI> candidates = new ArrayList<>();
        String configuredBaseUrl = properties.getAmap().getBaseUrl();
        candidates.add(buildGeocodeUri(configuredBaseUrl, city));
        if (configuredBaseUrl.startsWith("https://")) {
            candidates.add(buildGeocodeUri(configuredBaseUrl.replace("https://", "http://"), city));
        } else if (configuredBaseUrl.startsWith("http://")) {
            candidates.add(buildGeocodeUri(configuredBaseUrl.replace("http://", "https://"), city));
        }
        return deduplicateUris(candidates);
    }

    private List<URI> buildWeatherCandidateUris(String adcode) {
        List<URI> candidates = new ArrayList<>();
        String configuredBaseUrl = properties.getAmap().getBaseUrl();
        candidates.add(buildWeatherUri(configuredBaseUrl, adcode));
        if (configuredBaseUrl.startsWith("https://")) {
            candidates.add(buildWeatherUri(configuredBaseUrl.replace("https://", "http://"), adcode));
        } else if (configuredBaseUrl.startsWith("http://")) {
            candidates.add(buildWeatherUri(configuredBaseUrl.replace("http://", "https://"), adcode));
        }
        return deduplicateUris(candidates);
    }

    private URI buildGeocodeUri(String baseUrl, String city) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v3/geocode/geo")
                .queryParam("key", properties.getAmap().getApiKey())
                .queryParam("address", city)
                .queryParam("city", city)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    private URI buildWeatherUri(String baseUrl, String adcode) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v3/weather/weatherInfo")
                .queryParam("key", properties.getAmap().getApiKey())
                .queryParam("city", adcode)
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

    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String safeString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? defaultValue : text;
    }
}
