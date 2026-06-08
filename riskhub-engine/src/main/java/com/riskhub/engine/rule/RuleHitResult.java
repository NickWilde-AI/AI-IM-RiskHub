package com.riskhub.engine.rule;

import com.riskhub.common.dto.AuditSubmitRequest;

/**
 * 规则命中结果
 */
public class RuleHitResult {

    private String ruleId;
    private String riskTopic;
    private String riskLevel;
    private String actionHint;
    private String evidence;
    private String ruleVersion;

    public RuleHitResult() {}

    public RuleHitResult(String ruleId, String riskTopic, String riskLevel, String actionHint, String evidence, String ruleVersion) {
        this.ruleId = ruleId;
        this.riskTopic = riskTopic;
        this.riskLevel = riskLevel;
        this.actionHint = actionHint;
        this.evidence = evidence;
        this.ruleVersion = ruleVersion;
    }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRiskTopic() { return riskTopic; }
    public void setRiskTopic(String riskTopic) { this.riskTopic = riskTopic; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getActionHint() { return actionHint; }
    public void setActionHint(String actionHint) { this.actionHint = actionHint; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
}
