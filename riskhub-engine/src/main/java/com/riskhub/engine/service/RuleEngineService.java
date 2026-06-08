package com.riskhub.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.engine.rule.RuleHitResult;
import com.riskhub.engine.rule.RuleMatcher;
import com.riskhub.store.entity.RiskRuleEntity;
import com.riskhub.store.mapper.RiskRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 规则引擎服务：加载规则、执行匹配
 */
@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    private static final String RULE_CACHE_KEY = "riskhub:rules:enabled";

    private final RiskRuleMapper riskRuleMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RuleEngineService(RiskRuleMapper riskRuleMapper,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.riskRuleMapper = riskRuleMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行规则匹配，返回所有命中规则（按优先级排序）
     */
    public List<RuleHitResult> executeRules(AuditSubmitRequest request) {
        List<RiskRuleEntity> rules = loadEnabledRules();
        List<RuleHitResult> hits = new ArrayList<>();

        for (RiskRuleEntity rule : rules) {
            try {
                JsonNode conditionNode = objectMapper.readTree(rule.getConditionExpr());
                RuleHitResult hit = RuleMatcher.match(rule, request, conditionNode);
                if (hit != null) {
                    hits.add(hit);
                }
            } catch (Exception e) {
                log.warn("规则[{}]解析异常: {}", rule.getRuleId(), e.getMessage());
            }
        }
        return hits;
    }

    /**
     * 加载启用的规则，优先从数据库查询（生产环境可加 Redis 缓存）
     */
    private List<RiskRuleEntity> loadEnabledRules() {
        LambdaQueryWrapper<RiskRuleEntity> query = new LambdaQueryWrapper<>();
        query.eq(RiskRuleEntity::getEnabled, true)
             .orderByDesc(RiskRuleEntity::getPriority);
        return riskRuleMapper.selectList(query);
    }
}
