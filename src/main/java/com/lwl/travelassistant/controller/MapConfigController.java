package com.lwl.travelassistant.controller;

import com.lwl.travelassistant.config.TravelProviderProperties;
import com.lwl.travelassistant.model.MapConfigResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "*")
public class MapConfigController {

    private final TravelProviderProperties properties;

    public MapConfigController(TravelProviderProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public MapConfigResponse getMapConfig() {
        String webJsKey = properties.getAmap().getWebJsKey();
        boolean enabled = webJsKey != null && !webJsKey.isBlank();
        return new MapConfigResponse(
                enabled,
                enabled ? webJsKey : "",
                properties.getAmap().getSecurityJsCode(),
                "amap-jsapi",
                enabled
                        ? "高德地图前端配置已就绪"
                        : "尚未配置 AMAP_WEB_JS_KEY，当前地图暂显示占位提示"
        );
    }
}
