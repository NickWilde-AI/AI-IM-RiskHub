package com.riskhub.common.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 审核提交请求 DTO
 */
public class AuditSubmitRequest {

    @NotBlank(message = "request_id不能为空")
    private String requestId;

    @NotBlank(message = "biz_type不能为空")
    private String bizType;

    @NotBlank(message = "scene不能为空")
    private String scene;

    @NotBlank(message = "user_id不能为空")
    private String userId;

    private String contentId;
    private String contentText;
    private List<String> chatEvidenceList;
    private Map<String, Object> behaviorFeatures;
    private String callbackUrl;

    @NotBlank(message = "mode不能为空")
    private String mode;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public List<String> getChatEvidenceList() { return chatEvidenceList; }
    public void setChatEvidenceList(List<String> chatEvidenceList) { this.chatEvidenceList = chatEvidenceList; }
    public Map<String, Object> getBehaviorFeatures() { return behaviorFeatures; }
    public void setBehaviorFeatures(Map<String, Object> behaviorFeatures) { this.behaviorFeatures = behaviorFeatures; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
