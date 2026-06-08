package com.riskhub.common.enums;

/**
 * 处置动作枚举
 */
public enum AuditAction {
    IGNORE("ignore", "放行"),
    WARNING("warning", "警告"),
    LIMIT_ACCOUNT("limit_account", "限制账号能力"),
    HUMAN_REVIEW("human_review", "人工复核"),
    BAN_CANDIDATE("ban_candidate", "封禁候选"),
    REJECT_CONTENT("reject_content", "拦截内容"),
    SHADOW_ONLY("shadow_only", "只记录不执行");

    private final String code;
    private final String desc;

    AuditAction(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static AuditAction fromCode(String code) {
        for (AuditAction action : values()) {
            if (action.code.equals(code)) return action;
        }
        return IGNORE;
    }
}
