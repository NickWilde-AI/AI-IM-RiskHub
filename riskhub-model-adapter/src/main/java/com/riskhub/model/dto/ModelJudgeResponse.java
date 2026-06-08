package com.riskhub.model.dto;

/**
 * 模型服务响应 DTO（对应 AI-IM-Guard-ML /judge 接口返回）
 */
public class ModelJudgeResponse {

    private String modelName;
    private String modelVersion;
    private String riskLevel;
    private String finalJudgment;
    private String handlingSuggestion;
    private String topic;
    private Double confidence;
    private String reason;

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getFinalJudgment() { return finalJudgment; }
    public void setFinalJudgment(String finalJudgment) { this.finalJudgment = finalJudgment; }
    public String getHandlingSuggestion() { return handlingSuggestion; }
    public void setHandlingSuggestion(String handlingSuggestion) { this.handlingSuggestion = handlingSuggestion; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
