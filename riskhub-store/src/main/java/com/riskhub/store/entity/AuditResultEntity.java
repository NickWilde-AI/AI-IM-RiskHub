package com.riskhub.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 审核结果实体
 */
@TableName("audit_result")
public class AuditResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;
    private String riskTopic;
    private String riskLevel;
    private String finalJudgment;
    private String action;
    private String routeReason;
    private String ruleVersion;
    private String policyVersion;
    private String modelVersion;
    private Integer latencyMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
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
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
