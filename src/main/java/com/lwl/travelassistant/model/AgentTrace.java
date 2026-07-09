package com.lwl.travelassistant.model;

public class AgentTrace {

    private String agentName;
    private String action;
    private String inputSummary;
    private String outputSummary;
    private String status;

    public AgentTrace() {
    }

    public AgentTrace(String agentName,
                      String action,
                      String inputSummary,
                      String outputSummary,
                      String status) {
        this.agentName = agentName;
        this.action = action;
        this.inputSummary = inputSummary;
        this.outputSummary = outputSummary;
        this.status = status;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
