package com.riskhub.api.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskhub.common.dto.AuditSubmitRequest;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 异步审核消息生产者
 * 将异步审核请求投递到 RocketMQ
 * RocketMQ 不可用时 send 会抛异常，由调用方降级处理
 */
@Component
public class AuditMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditMessageProducer.class);
    public static final String TOPIC = "AUDIT_ASYNC_TOPIC";

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    private final ObjectMapper objectMapper;

    public AuditMessageProducer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 发送异步审核消息
     * @return true 发送成功，false MQ不可用
     */
    public boolean send(AuditSubmitRequest request) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ 不可用，异步消息无法投递 requestId={}", request.getRequestId());
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(request);
            rocketMQTemplate.syncSend(TOPIC, MessageBuilder.withPayload(json).build());
            log.info("异步审核消息已投递 requestId={} topic={}", request.getRequestId(), TOPIC);
            return true;
        } catch (JsonProcessingException e) {
            log.error("序列化异步审核消息失败 requestId={}", request.getRequestId(), e);
            return false;
        } catch (Exception e) {
            log.warn("RocketMQ 发送失败，降级为同步 requestId={} error={}", request.getRequestId(), e.getMessage());
            return false;
        }
    }
}
