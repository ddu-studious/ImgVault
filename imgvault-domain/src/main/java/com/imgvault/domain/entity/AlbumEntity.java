package com.imgvault.domain.entity;

import lombok.Data;

/**
 * 相册实体
 */
@Data
public class AlbumEntity {

    private Long id;
    private String name;
    private String description;
    private Long coverImageId;
    private Long ownerId;
    private Integer imageCount;
    private Integer accessLevel;
    private String createdAt;
    private String updatedAt;
}
