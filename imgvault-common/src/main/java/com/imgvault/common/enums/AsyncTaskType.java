package com.imgvault.common.enums;

import lombok.Getter;

/**
 * 异步任务类型
 */
@Getter
public enum AsyncTaskType {

    EXIF_EXTRACT("exif_extract", "EXIF 元数据提取"),
    THUMBNAIL_GENERATE("thumbnail_generate", "缩略图生成"),
    FORMAT_CONVERT("format_convert", "格式转换"),
    IMAGE_HASH("image_hash", "感知哈希计算");

    private final String code;
    private final String description;

    AsyncTaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AsyncTaskType fromCode(String code) {
        for (AsyncTaskType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的任务类型: " + code);
    }
}
