package com.riskhub.api.controller;

import com.riskhub.api.mq.AuditMessageProducer;
import com.riskhub.api.service.AuditService;
import com.riskhub.common.dto.AuditResultResponse;
import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 审核接入 Controller
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditMessageProducer auditMessageProducer;

    public AuditController(AuditService auditService, AuditMessageProducer auditMessageProducer) {
        this.auditService = auditService;
        this.auditMessageProducer = auditMessageProducer;
    }

    /**
     * 提交审核请求
     * sync模式：同步执行审核，直接返回结果
     * async模式：投递MQ后快速返回accepted；MQ不可用时降级为同步执行
     */
    @PostMapping("/submit")
    public Result<AuditResultResponse> submit(@Valid @RequestBody AuditSubmitRequest request) {
        AuditResultResponse response = auditService.submit(request);

        // 异步模式：尝试投递 MQ，失败则降级同步
        if ("async".equals(request.getMode()) && "accepted".equals(response.getStatus())) {
            boolean sent = auditMessageProducer.send(request);
            if (!sent) {
                // MQ 不可用，降级为同步执行
                response = auditService.submitInternal(request);
            }
        }

        return Result.success(response);
    }

    /**
     * 查询审核结果
     */
    @GetMapping("/result/{requestId}")
    public Result<AuditResultResponse> getResult(@PathVariable String requestId) {
        AuditResultResponse response = auditService.getResult(requestId);
        return Result.success(response);
    }
}
