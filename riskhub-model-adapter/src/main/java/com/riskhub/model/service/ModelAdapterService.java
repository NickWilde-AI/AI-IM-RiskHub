package com.riskhub.model.service;

import com.riskhub.common.dto.AuditSubmitRequest;
import com.riskhub.model.dto.ModelJudgeRequest;
import com.riskhub.model.dto.ModelJudgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 模型服务适配器
 * 调用 AI-IM-Guard-ML 的 /judge 接口，支持超时降级和 Shadow 模式。
 */
@Service
public class ModelAdapterService {

    private static final Logger log = LoggerFactory.getLogger(ModelAdapterService.class);

    @Value("${riskhub.model.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${riskhub.model.timeout-ms:3000}")
    private int timeoutMs;

    @Value("${riskhub.model.shadow-mode:false}")
    private boolean shadowMode;

    private final RestTemplate restTemplate;

    public ModelAdapterService() {
        this.restTemplate = new RestTemplate();
        // 设置连接和读取超时，模型不可用时快速失败
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(3000);
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * 调用模型服务
     * @return 模型判断结果，超时或异常时返回 null（走规则兜底）
     */
    public ModelJudgeResponse judge(AuditSubmitRequest request) {
        try {
            ModelJudgeRequest modelRequest = buildRequest(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ModelJudgeRequest> entity = new HttpEntity<>(modelRequest, headers);

            String url = baseUrl + "/judge";
            long start = System.currentTimeMillis();

            ModelJudgeResponse response = restTemplate.postForObject(url, entity, ModelJudgeResponse.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("模型调用成功 requestId={} model={} confidence={} latency={}ms shadow={}",
                    request.getRequestId(),
                    response != null ? response.getModelName() : "unknown",
                    response != null ? response.getConfidence() : 0,
                    elapsed,
                    shadowMode);

            return response;
        } catch (Exception e) {
            log.warn("模型调用失败，走规则兜底 requestId={} error={}",
                    request.getRequestId(), e.getMessage());
            return null;
        }
    }

    /**
     * 是否为 Shadow 模式（只记录不影响处置）
     */
    public boolean isShadowMode() {
        return shadowMode;
    }

    /**
     * 检查模型服务是否可用
     */
    public boolean isAvailable() {
        try {
            String url = baseUrl + "/health";
            String result = restTemplate.getForObject(url, String.class);
            return result != null && result.contains("ok");
        } catch (Exception e) {
            return false;
        }
    }

    private ModelJudgeRequest buildRequest(AuditSubmitRequest request) {
        ModelJudgeRequest modelReq = new ModelJudgeRequest();
        modelReq.setRequestId(request.getRequestId());
        modelReq.setScene(request.getScene());
        modelReq.setContentText(request.getContentText());
        modelReq.setChatEvidenceList(request.getChatEvidenceList());
        modelReq.setBehaviorFeatures(request.getBehaviorFeatures());
        return modelReq;
    }
}
