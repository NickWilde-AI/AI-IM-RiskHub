package com.riskhub.api.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskhub.api.service.AuditService;
import com.riskhub.common.dto.AuditSubmitRequest;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 异步审核消息消费者
 * 从 RocketMQ 消费审核任务并执行审核逻辑
 * 仅在 rocketmq.consumer.enabled=true 时注册
 */
@Component
@ConditionalOnProperty(name = "rocketmq.consumer.enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = AuditMessageProducer.TOPIC,
        consumerGroup = "riskhub-audit-consumer-group"
)
public class AuditMessageConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditMessageConsumer.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditMessageConsumer(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            AuditSubmitRequest request = objectMapper.readValue(message, AuditSubmitRequest.class);
            log.info("收到异步审核任务 requestId={}", request.getRequestId());

            // 执行审核逻辑（复用已有能力）
            auditService.submitInternal(request);

            log.info("异步审核完成 requestId={}", request.getRequestId());
        } catch (Exception e) {
            log.error("异步审核消费失败: {}", e.getMessage(), e);
            // 抛出异常触发 RocketMQ 重试
            throw new RuntimeException("异步审核处理失败", e);
        }
    }
}
