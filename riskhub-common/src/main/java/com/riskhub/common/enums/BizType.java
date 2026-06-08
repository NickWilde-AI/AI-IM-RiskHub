package com.riskhub.common.enums;

/**
 * 业务类型枚举
 */
public enum BizType {
    IM("im", "即时通讯"),
    COMMENT("comment", "评论"),
    LIVE("live", "直播"),
    COMMUNITY("community", "社区动态");

    private final String code;
    private final String desc;

    BizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static BizType fromCode(String code) {
        for (BizType type : values()) {
            if (type.code.equals(code)) return type;
        }
        return null;
    }
}
