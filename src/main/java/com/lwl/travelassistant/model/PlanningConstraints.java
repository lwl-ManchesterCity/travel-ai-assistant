package com.lwl.travelassistant.model;

import java.util.List;

public class PlanningConstraints {

    private boolean relaxedPace;
    private boolean avoidSeafood;
    private boolean wantsLandmarkCeremony;
    private boolean prefersIndoorBackup;
    private List<String> extractedTags;
    private String analysisSource;

    public PlanningConstraints() {
    }

    public PlanningConstraints(boolean relaxedPace,
                               boolean avoidSeafood,
                               boolean wantsLandmarkCeremony,
                               boolean prefersIndoorBackup,
                               List<String> extractedTags) {
        this.relaxedPace = relaxedPace;
        this.avoidSeafood = avoidSeafood;
        this.wantsLandmarkCeremony = wantsLandmarkCeremony;
        this.prefersIndoorBackup = prefersIndoorBackup;
        this.extractedTags = extractedTags;
        this.analysisSource = "rule";
    }

    public boolean isRelaxedPace() {
        return relaxedPace;
    }

    public void setRelaxedPace(boolean relaxedPace) {
        this.relaxedPace = relaxedPace;
    }

    public boolean isAvoidSeafood() {
        return avoidSeafood;
    }

    public void setAvoidSeafood(boolean avoidSeafood) {
        this.avoidSeafood = avoidSeafood;
    }

    public boolean isWantsLandmarkCeremony() {
        return wantsLandmarkCeremony;
    }

    public void setWantsLandmarkCeremony(boolean wantsLandmarkCeremony) {
        this.wantsLandmarkCeremony = wantsLandmarkCeremony;
    }

    public boolean isPrefersIndoorBackup() {
        return prefersIndoorBackup;
    }

    public void setPrefersIndoorBackup(boolean prefersIndoorBackup) {
        this.prefersIndoorBackup = prefersIndoorBackup;
    }

    public List<String> getExtractedTags() {
        return extractedTags;
    }

    public void setExtractedTags(List<String> extractedTags) {
        this.extractedTags = extractedTags;
    }

    public String getAnalysisSource() {
        return analysisSource;
    }

    public void setAnalysisSource(String analysisSource) {
        this.analysisSource = analysisSource;
    }
}
