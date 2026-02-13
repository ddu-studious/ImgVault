package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.ImageMetadataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 图片元数据 MyBatis Mapper
 */
@Mapper
public interface ImageMetadataMapper {

    int insert(ImageMetadataEntity entity);

    ImageMetadataEntity findByImageId(@Param("imageId") Long imageId);

    int deleteByImageId(@Param("imageId") Long imageId);
}
