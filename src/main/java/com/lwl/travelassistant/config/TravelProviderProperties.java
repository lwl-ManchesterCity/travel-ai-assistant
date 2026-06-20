package com.lwl.travelassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "travel.providers")
public class TravelProviderProperties {

    private String attraction = "mock";
    private String weather = "mock";
    private String hotel = "mock";
    private String route = "mock";
    private final Amap amap = new Amap();

    public String getAttraction() {
        return attraction;
    }

    public void setAttraction(String attraction) {
        this.attraction = attraction;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Amap getAmap() {
        return amap;
    }

    public static class Amap {

        private String apiKey = "";
        private String baseUrl = "https://restapi.amap.com";
        private boolean mockResponse = true;
        private String routeStrategy = "0";
        private String routeExtensions = "base";
        private boolean curlFallbackEnabled = true;
        private int poiPageSize = 10;
        private boolean cityLimit = true;
        private String webJsKey = "";
        private String securityJsCode = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isMockResponse() {
            return mockResponse;
        }

        public void setMockResponse(boolean mockResponse) {
            this.mockResponse = mockResponse;
        }

        public String getRouteStrategy() {
            return routeStrategy;
        }

        public void setRouteStrategy(String routeStrategy) {
            this.routeStrategy = routeStrategy;
        }

        public String getRouteExtensions() {
            return routeExtensions;
        }

        public void setRouteExtensions(String routeExtensions) {
            this.routeExtensions = routeExtensions;
        }

        public boolean isCurlFallbackEnabled() {
            return curlFallbackEnabled;
        }

        public void setCurlFallbackEnabled(boolean curlFallbackEnabled) {
            this.curlFallbackEnabled = curlFallbackEnabled;
        }

        public int getPoiPageSize() {
            return poiPageSize;
        }

        public void setPoiPageSize(int poiPageSize) {
            this.poiPageSize = poiPageSize;
        }

        public boolean isCityLimit() {
            return cityLimit;
        }

        public void setCityLimit(boolean cityLimit) {
            this.cityLimit = cityLimit;
        }

        public String getWebJsKey() {
            return webJsKey;
        }

        public void setWebJsKey(String webJsKey) {
            this.webJsKey = webJsKey;
        }

        public String getSecurityJsCode() {
            return securityJsCode;
        }

        public void setSecurityJsCode(String securityJsCode) {
            this.securityJsCode = securityJsCode;
        }
    }
}
