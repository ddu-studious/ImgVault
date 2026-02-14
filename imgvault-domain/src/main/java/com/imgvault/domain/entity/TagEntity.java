package com.imgvault.domain.entity;

import lombok.Data;

/**
 * 标签实体
 */
@Data
public class TagEntity {

    private Long id;
    private String name;
    private Integer imageCount;
    private String createdAt;
}
