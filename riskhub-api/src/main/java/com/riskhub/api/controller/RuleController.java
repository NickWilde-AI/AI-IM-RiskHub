package com.riskhub.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskhub.common.result.Result;
import com.riskhub.store.entity.RiskRuleEntity;
import com.riskhub.store.mapper.RiskRuleMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则管理 Controller
 */
@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RiskRuleMapper riskRuleMapper;

    public RuleController(RiskRuleMapper riskRuleMapper) {
        this.riskRuleMapper = riskRuleMapper;
    }

    /**
     * 查询所有规则
     */
    @GetMapping
    public Result<List<RiskRuleEntity>> list(@RequestParam(required = false) Boolean enabled) {
        LambdaQueryWrapper<RiskRuleEntity> query = new LambdaQueryWrapper<>();
        if (enabled != null) {
            query.eq(RiskRuleEntity::getEnabled, enabled);
        }
        query.orderByDesc(RiskRuleEntity::getPriority);
        return Result.success(riskRuleMapper.selectList(query));
    }

    /**
     * 查询单条规则
     */
    @GetMapping("/{ruleId}")
    public Result<RiskRuleEntity> get(@PathVariable String ruleId) {
        LambdaQueryWrapper<RiskRuleEntity> query = new LambdaQueryWrapper<>();
        query.eq(RiskRuleEntity::getRuleId, ruleId);
        RiskRuleEntity entity = riskRuleMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "规则不存在: " + ruleId);
        }
        return Result.success(entity);
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Result<RiskRuleEntity> create(@Valid @RequestBody RuleCreateRequest req) {
        RiskRuleEntity entity = new RiskRuleEntity();
        entity.setRuleId(req.getRuleId());
        entity.setRuleName(req.getRuleName());
        entity.setRiskTopic(req.getRiskTopic());
        entity.setConditionType(req.getConditionType());
        entity.setConditionExpr(req.getConditionExpr());
        entity.setRiskLevel(req.getRiskLevel());
        entity.setActionHint(req.getActionHint());
        entity.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setVersion(req.getVersion() != null ? req.getVersion() : "v1.0.0");
        riskRuleMapper.insert(entity);
        return Result.success(entity);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{ruleId}")
    public Result<RiskRuleEntity> update(@PathVariable String ruleId, @RequestBody RuleUpdateRequest req) {
        LambdaQueryWrapper<RiskRuleEntity> query = new LambdaQueryWrapper<>();
        query.eq(RiskRuleEntity::getRuleId, ruleId);
        RiskRuleEntity entity = riskRuleMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "规则不存在: " + ruleId);
        }
        if (req.getRuleName() != null) entity.setRuleName(req.getRuleName());
        if (req.getConditionExpr() != null) entity.setConditionExpr(req.getConditionExpr());
        if (req.getRiskLevel() != null) entity.setRiskLevel(req.getRiskLevel());
        if (req.getActionHint() != null) entity.setActionHint(req.getActionHint());
        if (req.getPriority() != null) entity.setPriority(req.getPriority());
        if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
        if (req.getVersion() != null) entity.setVersion(req.getVersion());
        riskRuleMapper.updateById(entity);
        return Result.success(entity);
    }

    /**
     * 启用/禁用规则
     */
    @PatchMapping("/{ruleId}/toggle")
    public Result<String> toggle(@PathVariable String ruleId) {
        LambdaQueryWrapper<RiskRuleEntity> query = new LambdaQueryWrapper<>();
        query.eq(RiskRuleEntity::getRuleId, ruleId);
        RiskRuleEntity entity = riskRuleMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "规则不存在: " + ruleId);
        }
        entity.setEnabled(!entity.getEnabled());
        riskRuleMapper.updateById(entity);
        return Result.success(entity.getEnabled() ? "已启用" : "已禁用");
    }

    // --- Request DTOs ---

    public static class RuleCreateRequest {
        @NotBlank(message = "rule_id不能为空")
        private String ruleId;
        @NotBlank(message = "rule_name不能为空")
        private String ruleName;
        @NotBlank(message = "risk_topic不能为空")
        private String riskTopic;
        @NotBlank(message = "condition_type不能为空")
        private String conditionType;
        @NotBlank(message = "condition_expr不能为空")
        private String conditionExpr;
        @NotBlank(message = "risk_level不能为空")
        private String riskLevel;
        @NotBlank(message = "action_hint不能为空")
        private String actionHint;
        private Integer priority;
        private Boolean enabled;
        private String version;

        public String getRuleId() { return ruleId; }
        public void setRuleId(String ruleId) { this.ruleId = ruleId; }
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getRiskTopic() { return riskTopic; }
        public void setRiskTopic(String riskTopic) { this.riskTopic = riskTopic; }
        public String getConditionType() { return conditionType; }
        public void setConditionType(String conditionType) { this.conditionType = conditionType; }
        public String getConditionExpr() { return conditionExpr; }
        public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getActionHint() { return actionHint; }
        public void setActionHint(String actionHint) { this.actionHint = actionHint; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public static class RuleUpdateRequest {
        private String ruleName;
        private String conditionExpr;
        private String riskLevel;
        private String actionHint;
        private Integer priority;
        private Boolean enabled;
        private String version;

        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        public String getConditionExpr() { return conditionExpr; }
        public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getActionHint() { return actionHint; }
        public void setActionHint(String actionHint) { this.actionHint = actionHint; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
