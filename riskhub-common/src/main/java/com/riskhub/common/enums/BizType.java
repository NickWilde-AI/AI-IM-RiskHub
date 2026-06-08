package com.riskhub.common.enums;

/**
 * 业务类型枚举
 */
public enum BizType {
    IM("im", "即时通讯"),
    COMMENT("comment", "评论"),
    LIVE("live", "直播"),
    COMMUNITY("community", "社区动态"),
    REGISTER("register", "注册"),
    LOGIN("login", "登录"),
    PROFILE("profile", "资料编辑"),
    MATCH("match", "匹配/社交"),
    FEED("feed", "动态/帖子"),
    PAYMENT("payment", "充值/交易");

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
