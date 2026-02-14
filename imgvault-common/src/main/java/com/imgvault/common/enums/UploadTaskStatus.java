package com.imgvault.common.enums;

import lombok.Getter;

/**
 * 分片上传任务状态
 */
@Getter
public enum UploadTaskStatus {

    UPLOADING("uploading", "上传中"),
    MERGING("merging", "合并中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "上传失败"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String description;

    UploadTaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UploadTaskStatus fromCode(String code) {
        for (UploadTaskStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的上传任务状态: " + code);
    }
}
