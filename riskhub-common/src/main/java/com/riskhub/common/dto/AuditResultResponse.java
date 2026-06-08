package com.riskhub.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 审核结果响应 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditResultResponse {

    private String requestId;
    private String status;
    private String riskTopic;
    private String riskLevel;
    private String finalJudgment;
    private String action;
    private String routeReason;
    private Integer latencyMs;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRiskTopic() { return riskTopic; }
    public void setRiskTopic(String riskTopic) { this.riskTopic = riskTopic; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getFinalJudgment() { return finalJudgment; }
    public void setFinalJudgment(String finalJudgment) { this.finalJudgment = finalJudgment; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRouteReason() { return routeReason; }
    public void setRouteReason(String routeReason) { this.routeReason = routeReason; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
}
