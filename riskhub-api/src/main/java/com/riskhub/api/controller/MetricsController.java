package com.riskhub.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskhub.common.result.Result;
import com.riskhub.store.entity.AuditRequestEntity;
import com.riskhub.store.entity.AuditResultEntity;
import com.riskhub.store.entity.ReviewTaskEntity;
import com.riskhub.store.mapper.AuditRequestMapper;
import com.riskhub.store.mapper.AuditResultMapper;
import com.riskhub.store.mapper.ReviewTaskMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控指标统计 Controller
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final AuditRequestMapper auditRequestMapper;
    private final AuditResultMapper auditResultMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final StringRedisTemplate redisTemplate;

    public MetricsController(AuditRequestMapper auditRequestMapper,
                             AuditResultMapper auditResultMapper,
                             ReviewTaskMapper reviewTaskMapper,
                             StringRedisTemplate redisTemplate) {
        this.auditRequestMapper = auditRequestMapper;
        this.auditResultMapper = auditResultMapper;
        this.reviewTaskMapper = reviewTaskMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 审核摘要统计（含中台独有指标）
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> summary = new HashMap<>();

        // === 基础指标 ===
        Long totalRequests = auditRequestMapper.selectCount(null);
        summary.put("totalRequests", totalRequests);

        List<AuditResultEntity> results = auditResultMapper.selectList(null);

        // 处置动作分布
        Map<String, Long> actionDistribution = results.stream()
                .filter(r -> r.getAction() != null)
                .collect(Collectors.groupingBy(AuditResultEntity::getAction, Collectors.counting()));
        summary.put("actionDistribution", actionDistribution);

        // 风险等级分布
        Map<String, Long> riskLevelDistribution = results.stream()
                .filter(r -> r.getRiskLevel() != null)
                .collect(Collectors.groupingBy(AuditResultEntity::getRiskLevel, Collectors.counting()));
        summary.put("riskLevelDistribution", riskLevelDistribution);

        // 风险主题分布
        Map<String, Long> topicDistribution = results.stream()
                .filter(r -> r.getRiskTopic() != null)
                .collect(Collectors.groupingBy(AuditResultEntity::getRiskTopic, Collectors.counting()));
        summary.put("topicDistribution", topicDistribution);

        // 平均耗时
        double avgLatency = results.stream()
                .filter(r -> r.getLatencyMs() != null)
                .mapToInt(AuditResultEntity::getLatencyMs)
                .average().orElse(0);
        summary.put("avgLatencyMs", Math.round(avgLatency));

        // === 中台独有指标 ===

        // 1. 双轨模式统计：AI 主审 vs 规则兜底
        long aiPrimaryCount = results.stream()
                .filter(r -> r.getModelVersion() != null && r.getModelVersion().length() > 0
                        && !"N/A".equals(r.getRuleVersion()))
                .count();
        long ruleFallbackCount = results.stream()
                .filter(r -> r.getModelVersion() == null || r.getModelVersion().isEmpty())
                .count();
        // 更精确：有 model_version 且 route_reason 以 "AI审核" 开头的是 AI 主审
        long aiCount = results.stream()
                .filter(r -> r.getRouteReason() != null && r.getRouteReason().startsWith("AI审核"))
                .count();
        long ruleCount = results.size() - aiCount;
        Map<String, Long> dualTrackMode = new HashMap<>();
        dualTrackMode.put("ai_primary", aiCount);
        dualTrackMode.put("rule_fallback", ruleCount);
        summary.put("dualTrackMode", dualTrackMode);

        // 2. 业务线分布
        List<AuditRequestEntity> requests = auditRequestMapper.selectList(null);
        Map<String, Long> bizTypeDistribution = requests.stream()
                .filter(r -> r.getBizType() != null)
                .collect(Collectors.groupingBy(AuditRequestEntity::getBizType, Collectors.counting()));
        summary.put("bizTypeDistribution", bizTypeDistribution);

        // 3. 同步/异步模式分布
        Map<String, Long> modeDistribution = requests.stream()
                .filter(r -> r.getMode() != null)
                .collect(Collectors.groupingBy(AuditRequestEntity::getMode, Collectors.counting()));
        summary.put("modeDistribution", modeDistribution);

        // 4. 延迟分段统计
        Map<String, Long> latencyBuckets = new HashMap<>();
        latencyBuckets.put("0-50ms", results.stream().filter(r -> r.getLatencyMs() != null && r.getLatencyMs() <= 50).count());
        latencyBuckets.put("50-100ms", results.stream().filter(r -> r.getLatencyMs() != null && r.getLatencyMs() > 50 && r.getLatencyMs() <= 100).count());
        latencyBuckets.put("100-500ms", results.stream().filter(r -> r.getLatencyMs() != null && r.getLatencyMs() > 100 && r.getLatencyMs() <= 500).count());
        latencyBuckets.put("500ms+", results.stream().filter(r -> r.getLatencyMs() != null && r.getLatencyMs() > 500).count());
        summary.put("latencyBuckets", latencyBuckets);

        // 5. 延迟百分位
        List<Integer> latencies = results.stream()
                .filter(r -> r.getLatencyMs() != null)
                .map(AuditResultEntity::getLatencyMs)
                .sorted()
                .collect(Collectors.toList());
        Map<String, Long> latencyPercentiles = new HashMap<>();
        if (!latencies.isEmpty()) {
            int n = latencies.size();
            latencyPercentiles.put("p50", (long) latencies.get((int)(n * 0.5)));
            latencyPercentiles.put("p95", (long) latencies.get(Math.min((int)(n * 0.95), n - 1)));
            latencyPercentiles.put("p99", (long) latencies.get(Math.min((int)(n * 0.99), n - 1)));
        }
        summary.put("latencyPercentiles", latencyPercentiles);

        // 6. 人工复核统计
        List<ReviewTaskEntity> allTasks = reviewTaskMapper.selectList(null);
        Map<String, Long> reviewStats = new HashMap<>();
        reviewStats.put("total", (long) allTasks.size());
        reviewStats.put("pending", allTasks.stream().filter(t -> "pending".equals(t.getStatus())).count());
        reviewStats.put("assigned", allTasks.stream().filter(t -> "assigned".equals(t.getStatus())).count());
        reviewStats.put("approved", allTasks.stream().filter(t -> "approved".equals(t.getStatus())).count());
        reviewStats.put("rejected", allTasks.stream().filter(t -> "rejected".equals(t.getStatus())).count());
        summary.put("reviewStats", reviewStats);

        // 7. 幂等拦截统计（基于 Redis keys 数量估算）
        Long idempotentKeys = 0L;
        try {
            var keys = redisTemplate.keys("riskhub:idempotent:*");
            idempotentKeys = keys != null ? (long) keys.size() : 0L;
        } catch (Exception ignored) {}
        summary.put("idempotentActiveKeys", idempotentKeys);

        // 8. 策略版本命中分布
        Map<String, Long> policyVersionDistribution = results.stream()
                .filter(r -> r.getPolicyVersion() != null)
                .collect(Collectors.groupingBy(AuditResultEntity::getPolicyVersion, Collectors.counting()));
        summary.put("policyVersionDistribution", policyVersionDistribution);

        // 9. 待复核任务数
        LambdaQueryWrapper<ReviewTaskEntity> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(ReviewTaskEntity::getStatus, "pending");
        Long pendingReviews = reviewTaskMapper.selectCount(pendingQuery);
        summary.put("pendingReviewTasks", pendingReviews);

        return Result.success(summary);
    }
}
