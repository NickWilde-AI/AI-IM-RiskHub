package com.riskhub.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 策略配置实体
 */
@TableName("policy_config")
public class PolicyConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String policyId;
    private String policyName;
    private String bizType;
    private String riskTopic;
    private String riskLevel;
    private String action;
    private Integer grayRatio;
    private Boolean enabled;
    private String version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public String getRiskTopic() { return riskTopic; }
    public void setRiskTopic(String riskTopic) { this.riskTopic = riskTopic; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getGrayRatio() { return grayRatio; }
    public void setGrayRatio(Integer grayRatio) { this.grayRatio = grayRatio; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
