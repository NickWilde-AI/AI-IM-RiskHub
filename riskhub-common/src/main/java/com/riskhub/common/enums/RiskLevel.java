package com.riskhub.common.enums;

/**
 * 风险等级枚举
 */
public enum RiskLevel {
    NO_RISK("no_risk", "无风险"),
    LOW_RISK("low_risk", "低风险"),
    MID_RISK("mid_risk", "中风险"),
    HIGH_RISK("high_risk", "高风险");

    private final String code;
    private final String desc;

    RiskLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static RiskLevel fromCode(String code) {
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) return level;
        }
        return NO_RISK;
    }
}
