package com.riskhub.api.controller;

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

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 提交审核请求
     */
    @PostMapping("/submit")
    public Result<AuditResultResponse> submit(@Valid @RequestBody AuditSubmitRequest request) {
        AuditResultResponse response = auditService.submit(request);
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
