package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传返回 DTO
 */
@Data
@Schema(description = "图片上传返回")
public class ImageUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "图片ID")
    private Long id;

    @Schema(description = "图片UUID")
    private String imageUuid;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "图片格式")
    private String format;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "存储路径")
    private String storagePath;

    @Schema(description = "下载URL")
    private String downloadUrl;

    @Schema(description = "文件SHA-256哈希")
    private String fileHash;

    @Schema(description = "是否为重复图片（已存在相同哈希的图片）")
    private Boolean duplicate;
}
