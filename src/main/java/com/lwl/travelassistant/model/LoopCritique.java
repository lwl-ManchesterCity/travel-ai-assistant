package com.lwl.travelassistant.model;

import java.util.List;

public class LoopCritique {

    private String action;
    private String reason;
    private String expectedOutcome;
    private String source;
    private List<String> notes;

    public LoopCritique() {
    }

    public LoopCritique(String action,
                        String reason,
                        String expectedOutcome,
                        String source,
                        List<String> notes) {
        this.action = action;
        this.reason = reason;
        this.expectedOutcome = expectedOutcome;
        this.source = source;
        this.notes = notes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public boolean isShouldReplan() {
        return action != null && !"STOP".equalsIgnoreCase(action);
    }
}
