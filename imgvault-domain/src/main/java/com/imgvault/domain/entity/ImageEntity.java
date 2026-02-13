package com.imgvault.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片实体
 */
@Data
public class ImageEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String imageUuid;
    private String fileHash;
    private String fileMd5;
    private String originalName;
    private String storagePath;
    private String bucketName;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String format;
    private String mimeType;
    private String colorSpace;
    private Integer hasAlpha;
    private Long uploaderId;
    private String uploadSource;
    private Integer status;          // 0-删除 1-正常 2-审核中
    private Integer accessLevel;     // 0-公开 1-私有 2-受限
    private Long viewCount;
    private String description;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
