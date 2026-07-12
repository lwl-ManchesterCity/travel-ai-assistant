package com.lwl.travelassistant.model;

public class LoopExecutionSummary {

    private boolean enabled;
    private int roundsExecuted;
    private int selectedRound;
    private String stopReason;

    public LoopExecutionSummary() {
    }

    public LoopExecutionSummary(boolean enabled, int roundsExecuted, int selectedRound, String stopReason) {
        this.enabled = enabled;
        this.roundsExecuted = roundsExecuted;
        this.selectedRound = selectedRound;
        this.stopReason = stopReason;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRoundsExecuted() {
        return roundsExecuted;
    }

    public void setRoundsExecuted(int roundsExecuted) {
        this.roundsExecuted = roundsExecuted;
    }

    public int getSelectedRound() {
        return selectedRound;
    }

    public void setSelectedRound(int selectedRound) {
        this.selectedRound = selectedRound;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }
}
