package com.lwl.travelassistant.model;

public class MapConfigResponse {

    private boolean enabled;
    private String jsApiKey;
    private String securityJsCode;
    private String provider;
    private String message;

    public MapConfigResponse() {
    }

    public MapConfigResponse(boolean enabled,
                             String jsApiKey,
                             String securityJsCode,
                             String provider,
                             String message) {
        this.enabled = enabled;
        this.jsApiKey = jsApiKey;
        this.securityJsCode = securityJsCode;
        this.provider = provider;
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJsApiKey() {
        return jsApiKey;
    }

    public void setJsApiKey(String jsApiKey) {
        this.jsApiKey = jsApiKey;
    }

    public String getSecurityJsCode() {
        return securityJsCode;
    }

    public void setSecurityJsCode(String securityJsCode) {
        this.securityJsCode = securityJsCode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
