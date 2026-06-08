package com.riskhub.model.dto;

import java.util.List;
import java.util.Map;

/**
 * 模型服务请求 DTO（对应 AI-IM-Guard-ML /judge 接口）
 */
public class ModelJudgeRequest {

    private String requestId;
    private String scene;
    private String contentText;
    private List<String> chatEvidenceList;
    private Map<String, Object> behaviorFeatures;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public List<String> getChatEvidenceList() { return chatEvidenceList; }
    public void setChatEvidenceList(List<String> chatEvidenceList) { this.chatEvidenceList = chatEvidenceList; }
    public Map<String, Object> getBehaviorFeatures() { return behaviorFeatures; }
    public void setBehaviorFeatures(Map<String, Object> behaviorFeatures) { this.behaviorFeatures = behaviorFeatures; }
}
