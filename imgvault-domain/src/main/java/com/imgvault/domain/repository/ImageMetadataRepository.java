package com.imgvault.domain.repository;

import com.imgvault.domain.entity.ImageMetadataEntity;

/**
 * 图片元数据仓库接口
 */
public interface ImageMetadataRepository {

    /**
     * 插入元数据
     */
    int insert(ImageMetadataEntity entity);

    /**
     * 根据图片 ID 查询元数据
     */
    ImageMetadataEntity findByImageId(Long imageId);

    /**
     * 根据图片 ID 删除元数据
     */
    int deleteByImageId(Long imageId);
}
