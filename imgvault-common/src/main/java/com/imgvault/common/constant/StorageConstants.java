package com.imgvault.common.constant;

/**
 * 存储相关常量
 */
public final class StorageConstants {

    private StorageConstants() {
    }

    /** 默认 Bucket 名称 */
    public static final String DEFAULT_BUCKET = "imgvault";

    /** 原图存储前缀 */
    public static final String ORIGINALS_PREFIX = "originals";

    /** 临时文件前缀 */
    public static final String TEMP_PREFIX = "temp";

    /** 头像前缀 */
    public static final String AVATARS_PREFIX = "avatars";

    /** 预签名 URL 默认有效期（秒） */
    public static final int PRESIGNED_URL_EXPIRY_SECONDS = 3600;

    /** 最大文件大小：50MB */
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    /** 分片大小：5MB */
    public static final int CHUNK_SIZE = 5 * 1024 * 1024;
}
