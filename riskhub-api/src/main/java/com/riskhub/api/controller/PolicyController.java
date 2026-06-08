package com.riskhub.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskhub.common.result.Result;
import com.riskhub.store.entity.PolicyConfigEntity;
import com.riskhub.store.mapper.PolicyConfigMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 策略管理 Controller
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyConfigMapper policyConfigMapper;

    public PolicyController(PolicyConfigMapper policyConfigMapper) {
        this.policyConfigMapper = policyConfigMapper;
    }

    /**
     * 查询所有策略
     */
    @GetMapping
    public Result<List<PolicyConfigEntity>> list(@RequestParam(required = false) String bizType) {
        LambdaQueryWrapper<PolicyConfigEntity> query = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isBlank()) {
            query.eq(PolicyConfigEntity::getBizType, bizType);
        }
        query.orderByDesc(PolicyConfigEntity::getEnabled);
        return Result.success(policyConfigMapper.selectList(query));
    }

    /**
     * 查询单条策略
     */
    @GetMapping("/{policyId}")
    public Result<PolicyConfigEntity> get(@PathVariable String policyId) {
        LambdaQueryWrapper<PolicyConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(PolicyConfigEntity::getPolicyId, policyId);
        PolicyConfigEntity entity = policyConfigMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "策略不存在: " + policyId);
        }
        return Result.success(entity);
    }

    /**
     * 发布策略
     */
    @PostMapping("/publish")
    public Result<PolicyConfigEntity> publish(@Valid @RequestBody PolicyPublishRequest req) {
        PolicyConfigEntity entity = new PolicyConfigEntity();
        entity.setPolicyId(req.getPolicyId());
        entity.setPolicyName(req.getPolicyName());
        entity.setBizType(req.getBizType());
        entity.setRiskTopic(req.getRiskTopic());
        entity.setRiskLevel(req.getRiskLevel());
        entity.setAction(req.getAction());
        entity.setGrayRatio(req.getGrayRatio() != null ? req.getGrayRatio() : 100);
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setVersion(req.getVersion() != null ? req.getVersion() : "v1.0.0");
        policyConfigMapper.insert(entity);
        return Result.success(entity);
    }

    /**
     * 更新策略
     */
    @PutMapping("/{policyId}")
    public Result<PolicyConfigEntity> update(@PathVariable String policyId, @RequestBody PolicyUpdateRequest req) {
        LambdaQueryWrapper<PolicyConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(PolicyConfigEntity::getPolicyId, policyId);
        PolicyConfigEntity entity = policyConfigMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "策略不存在: " + policyId);
        }
        if (req.getAction() != null) entity.setAction(req.getAction());
        if (req.getGrayRatio() != null) entity.setGrayRatio(req.getGrayRatio());
        if (req.getEnabled() != null) entity.setEnabled(req.getEnabled());
        if (req.getVersion() != null) entity.setVersion(req.getVersion());
        policyConfigMapper.updateById(entity);
        return Result.success(entity);
    }

    /**
     * 策略回滚（修改版本号 + 灰度归零）
     */
    @PostMapping("/{policyId}/rollback")
    public Result<String> rollback(@PathVariable String policyId) {
        LambdaQueryWrapper<PolicyConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(PolicyConfigEntity::getPolicyId, policyId);
        PolicyConfigEntity entity = policyConfigMapper.selectOne(query);
        if (entity == null) {
            return Result.fail(404, "策略不存在: " + policyId);
        }
        entity.setEnabled(false);
        entity.setGrayRatio(0);
        policyConfigMapper.updateById(entity);
        return Result.success("策略已回滚(禁用): " + policyId);
    }

    // --- Request DTOs ---

    public static class PolicyPublishRequest {
        @NotBlank(message = "policy_id不能为空")
        private String policyId;
        @NotBlank(message = "policy_name不能为空")
        private String policyName;
        @NotBlank(message = "biz_type不能为空")
        private String bizType;
        private String riskTopic;
        @NotBlank(message = "risk_level不能为空")
        private String riskLevel;
        @NotBlank(message = "action不能为空")
        private String action;
        private Integer grayRatio;
        private Boolean enabled;
        private String version;

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
    }

    public static class PolicyUpdateRequest {
        private String action;
        private Integer grayRatio;
        private Boolean enabled;
        private String version;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Integer getGrayRatio() { return grayRatio; }
        public void setGrayRatio(Integer grayRatio) { this.grayRatio = grayRatio; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
