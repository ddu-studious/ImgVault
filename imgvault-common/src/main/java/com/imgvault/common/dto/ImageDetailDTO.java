package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 图片详情 DTO
 */
@Data
@Schema(description = "图片详情")
public class ImageDetailDTO implements Serializable {

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

    @Schema(description = "色彩空间")
    private String colorSpace;

    @Schema(description = "是否有透明通道")
    private Boolean hasAlpha;

    @Schema(description = "图片状态: 0-删除 1-正常 2-审核中")
    private Integer status;

    @Schema(description = "访问级别: 0-公开 1-私有 2-受限")
    private Integer accessLevel;

    @Schema(description = "浏览次数")
    private Long viewCount;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "下载URL(预签名)")
    private String downloadUrl;

    @Schema(description = "文件SHA-256哈希")
    private String fileHash;

    @Schema(description = "缩略图URLs (imgproxy 实时处理)")
    private Map<String, String> thumbnails;

    @Schema(description = "格式变体URLs (WebP/AVIF)")
    private Map<String, String> variants;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;
}
