SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- AI-IM-RiskHub 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS riskhub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE riskhub;

-- 审核请求表
CREATE TABLE IF NOT EXISTS audit_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL COMMENT '全局请求ID',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型: im/comment/live/community',
    scene VARCHAR(64) NOT NULL COMMENT '审核场景',
    user_id_hash VARCHAR(64) NOT NULL COMMENT '用户ID hash',
    content_id VARCHAR(64) DEFAULT NULL COMMENT '内容ID',
    content_text TEXT DEFAULT NULL COMMENT '文本内容',
    mode VARCHAR(16) NOT NULL COMMENT 'sync/async',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id),
    INDEX idx_biz_type_created (biz_type, created_at),
    INDEX idx_user_id_hash_created (user_id_hash, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核请求表';

-- 审核结果表
CREATE TABLE IF NOT EXISTS audit_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    risk_topic VARCHAR(64) DEFAULT NULL COMMENT '风险主题',
    risk_level VARCHAR(32) DEFAULT NULL COMMENT '风险等级: no_risk/low_risk/mid_risk/high_risk',
    final_judgment VARCHAR(32) DEFAULT NULL COMMENT '是否违规: no_violation/exist_violation/suspected',
    action VARCHAR(32) NOT NULL COMMENT '处置动作',
    route_reason TEXT DEFAULT NULL COMMENT '路由原因',
    rule_version VARCHAR(32) DEFAULT NULL COMMENT '规则版本',
    policy_version VARCHAR(32) DEFAULT NULL COMMENT '策略版本',
    model_version VARCHAR(32) DEFAULT NULL COMMENT '模型版本',
    latency_ms INT DEFAULT NULL COMMENT '处理耗时(ms)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_request_id (request_id),
    INDEX idx_risk_topic_created (risk_topic, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核结果表';

-- 风险规则表
CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(64) NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    risk_topic VARCHAR(64) NOT NULL COMMENT '风险主题',
    condition_type VARCHAR(32) NOT NULL COMMENT '条件类型: keyword/regex/blacklist/whitelist/behavior_threshold/composite',
    condition_expr TEXT NOT NULL COMMENT '条件表达式(JSON)',
    risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
    action_hint VARCHAR(32) NOT NULL COMMENT '建议动作',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，数字越大优先级越高',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    version VARCHAR(32) NOT NULL DEFAULT 'v1.0.0' COMMENT '规则版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_id (rule_id),
    INDEX idx_enabled_priority (enabled, priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险规则表';

-- 策略配置表
CREATE TABLE IF NOT EXISTS policy_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id VARCHAR(64) NOT NULL COMMENT '策略ID',
    policy_name VARCHAR(128) NOT NULL COMMENT '策略名称',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    risk_topic VARCHAR(64) DEFAULT NULL COMMENT '风险主题',
    risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
    action VARCHAR(32) NOT NULL COMMENT '处置动作',
    gray_ratio INT NOT NULL DEFAULT 100 COMMENT '灰度比例(0-100)',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    version VARCHAR(32) NOT NULL DEFAULT 'v1.0.0' COMMENT '策略版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy_id (policy_id),
    INDEX idx_biz_risk (biz_type, risk_topic, risk_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略配置表';

-- 插入示例规则数据
INSERT INTO risk_rule (rule_id, rule_name, risk_topic, condition_type, condition_expr, risk_level, action_hint, priority, enabled, version) VALUES
('rule_keyword_001', '外部联系方式关键词', '诈骗引流', 'keyword', '{"keywords": ["加微信", "加QQ", "加V", "私聊发你", "扫码加"]}', 'mid_risk', 'warning', 10, 1, 'v1.0.0'),
('rule_keyword_002', '色情诱导关键词', '色情诱导', 'keyword', '{"keywords": ["约吗", "寂寞吗", "上门服务", "看片"]}', 'high_risk', 'reject_content', 20, 1, 'v1.0.0'),
('rule_regex_001', '手机号正则', '诈骗引流', 'regex', '{"pattern": "1[3-9]\\\\d{9}"}', 'low_risk', 'warning', 5, 1, 'v1.0.0'),
('rule_behavior_001', '高频外联行为', '诈骗引流', 'behavior_threshold', '{"field": "external_contact_count", "operator": ">=", "threshold": 10}', 'mid_risk', 'human_review', 15, 1, 'v1.0.0'),
('rule_composite_001', '引流+收益承诺组合', '诈骗引流', 'composite', '{"logic": "AND", "conditions": [{"type": "keyword", "keywords": ["加微信", "加V"]}, {"type": "keyword", "keywords": ["稳赚", "包赚", "日入", "躺赚"]}]}', 'high_risk', 'ban_candidate', 25, 1, 'v1.0.0');

-- 插入示例策略数据
INSERT INTO policy_config (policy_id, policy_name, biz_type, risk_topic, risk_level, action, gray_ratio, enabled, version) VALUES
('policy_im_high', 'IM高风险策略', 'im', NULL, 'high_risk', 'reject_content', 100, 1, 'v1.0.0'),
('policy_im_mid', 'IM中风险策略', 'im', NULL, 'mid_risk', 'human_review', 100, 1, 'v1.0.0'),
('policy_im_low', 'IM低风险策略', 'im', NULL, 'low_risk', 'warning', 50, 1, 'v1.0.0'),
('policy_comment_high', '评论高风险策略', 'comment', NULL, 'high_risk', 'reject_content', 100, 1, 'v1.0.0');

-- 人工复核任务表
CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    assignee VARCHAR(64) DEFAULT NULL COMMENT '审核员',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/assigned/processing/approved/rejected/escalated/expired',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    risk_topic VARCHAR(64) DEFAULT NULL COMMENT '风险主题',
    evidence_summary TEXT DEFAULT NULL COMMENT '证据摘要',
    review_result VARCHAR(32) DEFAULT NULL COMMENT '复核结论',
    review_reason TEXT DEFAULT NULL COMMENT '复核理由',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_status_priority (status, priority DESC),
    INDEX idx_assignee (assignee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工复核任务表';
