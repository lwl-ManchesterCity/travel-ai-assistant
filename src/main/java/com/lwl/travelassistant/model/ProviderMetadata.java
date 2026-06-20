package com.lwl.travelassistant.model;

public class ProviderMetadata {

    private String providerCode;
    private String providerName;
    private boolean mock;

    public ProviderMetadata() {
    }

    public ProviderMetadata(String providerCode, String providerName, boolean mock) {
        this.providerCode = providerCode;
        this.providerName = providerName;
        this.mock = mock;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }
}
