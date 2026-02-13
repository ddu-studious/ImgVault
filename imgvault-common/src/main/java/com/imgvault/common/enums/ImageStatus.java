package com.imgvault.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图片状态枚举
 */
@Getter
@AllArgsConstructor
public enum ImageStatus {

    DELETED(0, "已删除"),
    NORMAL(1, "正常"),
    REVIEWING(2, "审核中");

    private final int code;
    private final String desc;

    public static ImageStatus of(int code) {
        for (ImageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown image status: " + code);
    }
}
