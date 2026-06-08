package com.riskhub.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskhub.common.result.Result;
import com.riskhub.store.entity.AuditResultEntity;
import com.riskhub.store.entity.ReviewTaskEntity;
import com.riskhub.store.mapper.AuditRequestMapper;
import com.riskhub.store.mapper.AuditResultMapper;
import com.riskhub.store.mapper.ReviewTaskMapper;
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

    public MetricsController(AuditRequestMapper auditRequestMapper,
                             AuditResultMapper auditResultMapper,
                             ReviewTaskMapper reviewTaskMapper) {
        this.auditRequestMapper = auditRequestMapper;
        this.auditResultMapper = auditResultMapper;
        this.reviewTaskMapper = reviewTaskMapper;
    }

    /**
     * 审核摘要统计
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> summary = new HashMap<>();

        // 总请求量
        Long totalRequests = auditRequestMapper.selectCount(null);
        summary.put("totalRequests", totalRequests);

        // 审核结果分布
        List<AuditResultEntity> results = auditResultMapper.selectList(null);
        Map<String, Long> actionDistribution = results.stream()
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
                .average()
                .orElse(0);
        summary.put("avgLatencyMs", Math.round(avgLatency));

        // 待复核任务数
        LambdaQueryWrapper<ReviewTaskEntity> pendingQuery = new LambdaQueryWrapper<>();
        pendingQuery.eq(ReviewTaskEntity::getStatus, "pending");
        Long pendingReviews = reviewTaskMapper.selectCount(pendingQuery);
        summary.put("pendingReviewTasks", pendingReviews);

        return Result.success(summary);
    }
}
