package com.riskhub.api.service;

import com.riskhub.common.dto.AuditResultResponse;
import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.common.exception.BizException;
import com.riskhub.engine.rule.RuleHitResult;
import com.riskhub.engine.service.PolicyRouterService;
import com.riskhub.engine.service.PolicyRouterService.PolicyDecision;
import com.riskhub.engine.service.RuleEngineService;
import com.riskhub.model.dto.ModelJudgeResponse;
import com.riskhub.model.service.ModelAdapterService;
import com.riskhub.store.entity.AuditRequestEntity;
import com.riskhub.store.entity.AuditResultEntity;
import com.riskhub.store.mapper.AuditRequestMapper;
import com.riskhub.store.mapper.AuditResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 审核业务服务
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String IDEMPOTENT_KEY_PREFIX = "riskhub:idempotent:";
    private static final long IDEMPOTENT_EXPIRE_SECONDS = 3600;

    private final AuditRequestMapper auditRequestMapper;
    private final AuditResultMapper auditResultMapper;
    private final RuleEngineService ruleEngineService;
    private final PolicyRouterService policyRouterService;
    private final ModelAdapterService modelAdapterService;
    private final StringRedisTemplate redisTemplate;

    public AuditService(AuditRequestMapper auditRequestMapper,
                        AuditResultMapper auditResultMapper,
                        RuleEngineService ruleEngineService,
                        PolicyRouterService policyRouterService,
                        ModelAdapterService modelAdapterService,
                        StringRedisTemplate redisTemplate) {
        this.auditRequestMapper = auditRequestMapper;
        this.auditResultMapper = auditResultMapper;
        this.ruleEngineService = ruleEngineService;
        this.policyRouterService = policyRouterService;
        this.modelAdapterService = modelAdapterService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 提交审核请求入口（区分同步/异步）
     */
    public AuditResultResponse submit(AuditSubmitRequest request) {
        // 幂等检查
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + request.getRequestId();
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_EXPIRE_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(isNew)) {
            return getResult(request.getRequestId());
        }

        // 异步模式：保存请求后投递MQ，快速返回
        if ("async".equals(request.getMode())) {
            return submitAsync(request);
        }

        // 同步模式：直接执行审核
        return submitInternal(request);
    }

    /**
     * 异步提交：保存请求记录，返回 accepted 状态
     */
    private AuditResultResponse submitAsync(AuditSubmitRequest request) {
        // 保存审核请求（状态 pending）
        AuditRequestEntity requestEntity = buildRequestEntity(request);
        requestEntity.setStatus("pending");
        auditRequestMapper.insert(requestEntity);

        AuditResultResponse response = new AuditResultResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus("accepted");
        response.setAction("async_processing");
        response.setRouteReason("已投递异步审核队列");
        response.setLatencyMs(0);

        log.info("异步审核已接收 requestId={}", request.getRequestId());
        return response;
    }

    /**
     * 内部审核执行（同步模式 & 异步Consumer共用）
     *
     * 双轨模式：AI 主审 + 规则兜底
     * 1. 优先调用 AI 模型进行语义审核
     * 2. AI 可用且置信度高 → 直接采信 AI 结果，仅补充行为规则
     * 3. AI 不可用或置信度低 → 降级走全量规则引擎
     */
    public AuditResultResponse submitInternal(AuditSubmitRequest request) {
        long startTime = System.currentTimeMillis();

        // 保存或更新审核请求
        LambdaQueryWrapper<AuditRequestEntity> existQuery = new LambdaQueryWrapper<>();
        existQuery.eq(AuditRequestEntity::getRequestId, request.getRequestId());
        AuditRequestEntity requestEntity = auditRequestMapper.selectOne(existQuery);
        if (requestEntity == null) {
            requestEntity = buildRequestEntity(request);
            requestEntity.setStatus("processing");
            auditRequestMapper.insert(requestEntity);
        } else {
            requestEntity.setStatus("processing");
            auditRequestMapper.updateById(requestEntity);
        }

        // === 双轨模式：AI 主审 ===
        ModelJudgeResponse modelResponse = modelAdapterService.judge(request);
        String modelVersion = null;
        List<RuleHitResult> ruleHits;

        boolean aiPrimary = modelResponse != null && !modelAdapterService.isShadowMode()
                && modelResponse.getConfidence() != null && modelResponse.getConfidence() >= 0.8;

        if (aiPrimary) {
            // AI 可用且高置信度 → 采信 AI 结果
            modelVersion = modelResponse.getModelVersion();
            RuleHitResult modelHit = new RuleHitResult(
                    "model_" + (modelResponse.getModelName() != null ? modelResponse.getModelName() : "guard-ml"),
                    modelResponse.getTopic(),
                    modelResponse.getRiskLevel(),
                    modelResponse.getHandlingSuggestion(),
                    "AI审核: " + modelResponse.getReason(),
                    modelResponse.getModelVersion()
            );

            // 仅执行行为类规则作为补充（不执行关键词/正则等语义规则）
            List<RuleHitResult> behaviorHits = ruleEngineService.executeBehaviorRulesOnly(request);
            ruleHits = new java.util.ArrayList<>();
            ruleHits.add(modelHit); // AI 结果最高优先级
            ruleHits.addAll(behaviorHits); // 行为规则补充

            log.info("AI主审模式 requestId={} confidence={} riskLevel={} topic={}",
                    request.getRequestId(), modelResponse.getConfidence(),
                    modelResponse.getRiskLevel(), modelResponse.getTopic());
        } else {
            // AI 不可用或低置信度 → 降级走全量规则引擎
            if (modelResponse != null) {
                modelVersion = modelResponse.getModelVersion();
            }
            ruleHits = ruleEngineService.executeRules(request);
            log.info("规则兜底模式 requestId={} aiAvailable={} confidence={}",
                    request.getRequestId(), modelResponse != null,
                    modelResponse != null ? modelResponse.getConfidence() : "N/A");
        }

        // 策略路由
        PolicyDecision decision = policyRouterService.route(request, ruleHits);

        long latencyMs = System.currentTimeMillis() - startTime;

        // 保存审核结果
        AuditResultEntity resultEntity = new AuditResultEntity();
        resultEntity.setRequestId(request.getRequestId());
        resultEntity.setRiskTopic(decision.getRiskTopic());
        resultEntity.setRiskLevel(decision.getRiskLevel());
        resultEntity.setFinalJudgment(decision.getFinalJudgment());
        resultEntity.setAction(decision.getAction());
        resultEntity.setRouteReason(decision.getRouteReason());
        resultEntity.setRuleVersion(decision.getRuleVersion());
        resultEntity.setPolicyVersion(decision.getPolicyVersion());
        resultEntity.setModelVersion(modelVersion);
        resultEntity.setLatencyMs((int) latencyMs);
        auditResultMapper.insert(resultEntity);

        // 更新请求状态
        requestEntity.setStatus("completed");
        auditRequestMapper.updateById(requestEntity);

        // 构造响应
        AuditResultResponse response = new AuditResultResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus("completed");
        response.setRiskTopic(decision.getRiskTopic());
        response.setRiskLevel(decision.getRiskLevel());
        response.setFinalJudgment(decision.getFinalJudgment());
        response.setAction(decision.getAction());
        response.setRouteReason(decision.getRouteReason());
        response.setLatencyMs((int) latencyMs);

        log.info("审核完成 requestId={} action={} mode={} modelVersion={} latency={}ms",
                request.getRequestId(), decision.getAction(),
                aiPrimary ? "AI主审" : "规则兜底", modelVersion, latencyMs);
        return response;
    }

    /**
     * 查询审核结果
     */
    public AuditResultResponse getResult(String requestId) {
        LambdaQueryWrapper<AuditResultEntity> query = new LambdaQueryWrapper<>();
        query.eq(AuditResultEntity::getRequestId, requestId).last("LIMIT 1");
        AuditResultEntity entity = auditResultMapper.selectOne(query);

        if (entity == null) {
            // 可能是异步任务还在处理中
            LambdaQueryWrapper<AuditRequestEntity> reqQuery = new LambdaQueryWrapper<>();
            reqQuery.eq(AuditRequestEntity::getRequestId, requestId);
            AuditRequestEntity reqEntity = auditRequestMapper.selectOne(reqQuery);
            if (reqEntity != null && !"completed".equals(reqEntity.getStatus())) {
                AuditResultResponse response = new AuditResultResponse();
                response.setRequestId(requestId);
                response.setStatus(reqEntity.getStatus());
                response.setAction("processing");
                return response;
            }
            throw new BizException(404, "审核结果不存在: " + requestId);
        }

        AuditResultResponse response = new AuditResultResponse();
        response.setRequestId(requestId);
        response.setStatus("completed");
        response.setRiskTopic(entity.getRiskTopic());
        response.setRiskLevel(entity.getRiskLevel());
        response.setFinalJudgment(entity.getFinalJudgment());
        response.setAction(entity.getAction());
        response.setRouteReason(entity.getRouteReason());
        response.setLatencyMs(entity.getLatencyMs());
        return response;
    }

    private AuditRequestEntity buildRequestEntity(AuditSubmitRequest request) {
        AuditRequestEntity entity = new AuditRequestEntity();
        entity.setRequestId(request.getRequestId());
        entity.setBizType(request.getBizType());
        entity.setScene(request.getScene());
        entity.setUserIdHash(hashUserId(request.getUserId()));
        entity.setContentId(request.getContentId());
        entity.setContentText(request.getContentText());
        entity.setMode(request.getMode());
        return entity;
    }

    private String hashUserId(String userId) {
        if (userId == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(userId.substring(0, Math.min(userId.length(), 16))
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return userId;
        }
    }
}
