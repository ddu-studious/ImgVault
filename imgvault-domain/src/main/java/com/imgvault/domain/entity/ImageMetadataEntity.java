package com.imgvault.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片 EXIF 元数据实体
 */
@Data
public class ImageMetadataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long imageId;
    private String cameraMake;
    private String cameraModel;
    private String lensModel;
    private String focalLength;
    private String aperture;
    private String shutterSpeed;
    private Integer iso;
    private String takenAt;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private Integer orientation;
    private String rawExif;       // JSON 字符串
    private String createdAt;
}
