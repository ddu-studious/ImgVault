package com.imgvault.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 访问级别枚举
 */
@Getter
@AllArgsConstructor
public enum AccessLevel {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    RESTRICTED(2, "受限");

    private final int code;
    private final String desc;

    public static AccessLevel of(int code) {
        for (AccessLevel level : values()) {
            if (level.code == code) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown access level: " + code);
    }
}
