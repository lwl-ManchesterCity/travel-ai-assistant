package com.lwl.travelassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "travel.loop")
public class TravelLoopProperties {

    private boolean enabled = true;
    private int maxRounds = 3;
    private int maxNoImprovementRounds = 2;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getMaxNoImprovementRounds() {
        return maxNoImprovementRounds;
    }

    public void setMaxNoImprovementRounds(int maxNoImprovementRounds) {
        this.maxNoImprovementRounds = maxNoImprovementRounds;
    }
}
