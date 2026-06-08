package com.riskhub.common.enums;

/**
 * 审核模式枚举
 */
public enum AuditMode {
    SYNC("sync", "同步审核"),
    ASYNC("async", "异步审核");

    private final String code;
    private final String desc;

    AuditMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static AuditMode fromCode(String code) {
        for (AuditMode mode : values()) {
            if (mode.code.equals(code)) return mode;
        }
        return SYNC;
    }
}
