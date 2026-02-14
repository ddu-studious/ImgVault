package com.imgvault.common.enums;

import lombok.Getter;

/**
 * 异步任务状态
 */
@Getter
public enum AsyncTaskStatus {

    PENDING("pending", "待处理"),
    PROCESSING("processing", "处理中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    AsyncTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AsyncTaskStatus fromCode(String code) {
        for (AsyncTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的任务状态: " + code);
    }
}
