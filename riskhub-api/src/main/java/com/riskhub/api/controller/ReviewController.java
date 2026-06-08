package com.riskhub.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.riskhub.common.result.Result;
import com.riskhub.store.entity.ReviewTaskEntity;
import com.riskhub.store.mapper.ReviewTaskMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 人工复核 Controller
 */
@RestController
@RequestMapping("/api/v1/review/tasks")
public class ReviewController {

    private final ReviewTaskMapper reviewTaskMapper;

    public ReviewController(ReviewTaskMapper reviewTaskMapper) {
        this.reviewTaskMapper = reviewTaskMapper;
    }

    /**
     * 查询复核任务列表
     */
    @GetMapping
    public Result<List<ReviewTaskEntity>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignee) {
        LambdaQueryWrapper<ReviewTaskEntity> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            query.eq(ReviewTaskEntity::getStatus, status);
        }
        if (assignee != null && !assignee.isBlank()) {
            query.eq(ReviewTaskEntity::getAssignee, assignee);
        }
        query.orderByDesc(ReviewTaskEntity::getPriority)
             .orderByAsc(ReviewTaskEntity::getCreatedAt);
        return Result.success(reviewTaskMapper.selectList(query));
    }

    /**
     * 查询单个复核任务
     */
    @GetMapping("/{taskId}")
    public Result<ReviewTaskEntity> get(@PathVariable String taskId) {
        ReviewTaskEntity entity = findByTaskId(taskId);
        if (entity == null) {
            return Result.fail(404, "复核任务不存在: " + taskId);
        }
        return Result.success(entity);
    }

    /**
     * 创建复核任务（通常由审核流程自动触发）
     */
    @PostMapping
    public Result<ReviewTaskEntity> create(@Valid @RequestBody ReviewCreateRequest req) {
        ReviewTaskEntity entity = new ReviewTaskEntity();
        entity.setTaskId("review_" + UUID.randomUUID().toString().substring(0, 8));
        entity.setRequestId(req.getRequestId());
        entity.setStatus("pending");
        entity.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        entity.setRiskTopic(req.getRiskTopic());
        entity.setEvidenceSummary(req.getEvidenceSummary());
        reviewTaskMapper.insert(entity);
        return Result.success(entity);
    }

    /**
     * 领取任务
     */
    @PostMapping("/{taskId}/claim")
    public Result<ReviewTaskEntity> claim(@PathVariable String taskId, @RequestBody ClaimRequest req) {
        ReviewTaskEntity entity = findByTaskId(taskId);
        if (entity == null) {
            return Result.fail(404, "复核任务不存在: " + taskId);
        }
        if (!"pending".equals(entity.getStatus())) {
            return Result.fail(400, "任务状态不允许领取，当前状态: " + entity.getStatus());
        }
        entity.setAssignee(req.getAssignee());
        entity.setStatus("assigned");
        reviewTaskMapper.updateById(entity);
        return Result.success(entity);
    }

    /**
     * 提交复核结论
     */
    @PostMapping("/{taskId}/submit")
    public Result<ReviewTaskEntity> submit(@PathVariable String taskId, @Valid @RequestBody ReviewSubmitRequest req) {
        ReviewTaskEntity entity = findByTaskId(taskId);
        if (entity == null) {
            return Result.fail(404, "复核任务不存在: " + taskId);
        }
        if (!"assigned".equals(entity.getStatus()) && !"processing".equals(entity.getStatus())) {
            return Result.fail(400, "任务状态不允许提交，当前状态: " + entity.getStatus());
        }
        entity.setReviewResult(req.getReviewResult());
        entity.setReviewReason(req.getReviewReason());
        entity.setStatus(req.getReviewResult());
        entity.setFinishedAt(LocalDateTime.now());
        reviewTaskMapper.updateById(entity);
        return Result.success(entity);
    }

    private ReviewTaskEntity findByTaskId(String taskId) {
        LambdaQueryWrapper<ReviewTaskEntity> query = new LambdaQueryWrapper<>();
        query.eq(ReviewTaskEntity::getTaskId, taskId);
        return reviewTaskMapper.selectOne(query);
    }

    // --- Request DTOs ---

    public static class ReviewCreateRequest {
        @NotBlank(message = "request_id不能为空")
        private String requestId;
        private String riskTopic;
        private String evidenceSummary;
        private Integer priority;

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getRiskTopic() { return riskTopic; }
        public void setRiskTopic(String riskTopic) { this.riskTopic = riskTopic; }
        public String getEvidenceSummary() { return evidenceSummary; }
        public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    public static class ClaimRequest {
        private String assignee;
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
    }

    public static class ReviewSubmitRequest {
        @NotBlank(message = "review_result不能为空")
        private String reviewResult;
        private String reviewReason;

        public String getReviewResult() { return reviewResult; }
        public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
        public String getReviewReason() { return reviewReason; }
        public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    }
}
