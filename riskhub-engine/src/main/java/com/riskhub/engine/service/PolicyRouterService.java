package com.riskhub.engine.service;

import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.common.enums.AuditAction;
import com.riskhub.common.enums.RiskLevel;
import com.riskhub.engine.rule.RuleHitResult;
import com.riskhub.store.entity.PolicyConfigEntity;
import com.riskhub.store.mapper.PolicyConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 策略路由服务：根据规则命中结果 + 业务场景，决定最终处置动作
 */
@Service
public class PolicyRouterService {

    private static final Logger log = LoggerFactory.getLogger(PolicyRouterService.class);

    private final PolicyConfigMapper policyConfigMapper;

    public PolicyRouterService(PolicyConfigMapper policyConfigMapper) {
        this.policyConfigMapper = policyConfigMapper;
    }

    /**
     * 路由决策：综合规则命中结果，匹配策略，输出最终动作
     */
    public PolicyDecision route(AuditSubmitRequest request, List<RuleHitResult> ruleHits) {
        if (ruleHits == null || ruleHits.isEmpty()) {
            return PolicyDecision.pass();
        }

        // 取最高优先级（列表已排序）的命中结果
        RuleHitResult topHit = ruleHits.get(0);

        // 查找匹配的策略
        PolicyConfigEntity policy = findPolicy(request.getBizType(), topHit.getRiskLevel());

        String finalAction;
        String policyVersion;
        if (policy != null && hitGray(policy.getGrayRatio())) {
            finalAction = policy.getAction();
            policyVersion = policy.getVersion();
        } else {
            // 无匹配策略时使用规则建议动作
            finalAction = topHit.getActionHint();
            policyVersion = "default";
        }

        // 决定 finalJudgment
        String finalJudgment = determineFinalJudgment(topHit.getRiskLevel(), finalAction);

        PolicyDecision decision = new PolicyDecision();
        decision.setRiskTopic(topHit.getRiskTopic());
        decision.setRiskLevel(topHit.getRiskLevel());
        decision.setFinalJudgment(finalJudgment);
        decision.setAction(finalAction);
        decision.setRouteReason(topHit.getEvidence());
        decision.setRuleVersion(topHit.getRuleVersion());
        decision.setPolicyVersion(policyVersion);
        return decision;
    }

    private PolicyConfigEntity findPolicy(String bizType, String riskLevel) {
        LambdaQueryWrapper<PolicyConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(PolicyConfigEntity::getBizType, bizType)
             .eq(PolicyConfigEntity::getRiskLevel, riskLevel)
             .eq(PolicyConfigEntity::getEnabled, true)
             .last("LIMIT 1");
        return policyConfigMapper.selectOne(query);
    }

    private boolean hitGray(Integer grayRatio) {
        if (grayRatio == null || grayRatio >= 100) return true;
        if (grayRatio <= 0) return false;
        return ThreadLocalRandom.current().nextInt(100) < grayRatio;
    }

    private String determineFinalJudgment(String riskLevel, String action) {
        if ("reject_content".equals(action) || "ban_candidate".equals(action)) {
            return "exist_violation";
        }
        if ("human_review".equals(action)) {
            return "suspected";
        }
        if ("high_risk".equals(riskLevel)) {
            return "exist_violation";
        }
        if ("mid_risk".equals(riskLevel)) {
            return "suspected";
        }
        return "no_violation";
    }

    /**
     * 策略决策结果
     */
    public static class PolicyDecision {
        private String riskTopic;
        private String riskLevel;
        private String finalJudgment;
        private String action;
        private String routeReason;
        private String ruleVersion;
        private String policyVersion;

        public static PolicyDecision pass() {
            PolicyDecision d = new PolicyDecision();
            d.setRiskLevel("no_risk");
            d.setFinalJudgment("no_violation");
            d.setAction("ignore");
            d.setRouteReason("未命中任何规则");
            d.setRuleVersion("N/A");
            d.setPolicyVersion("N/A");
            return d;
        }

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
    }
}
