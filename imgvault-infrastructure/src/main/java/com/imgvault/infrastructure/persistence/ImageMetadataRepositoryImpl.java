package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.ImageMetadataEntity;
import com.imgvault.domain.repository.ImageMetadataRepository;
import com.imgvault.infrastructure.persistence.mapper.ImageMetadataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 图片元数据仓库实现
 */
@Repository
@RequiredArgsConstructor
public class ImageMetadataRepositoryImpl implements ImageMetadataRepository {

    private final ImageMetadataMapper metadataMapper;

    @Override
    public int insert(ImageMetadataEntity entity) {
        return metadataMapper.insert(entity);
    }

    @Override
    public ImageMetadataEntity findByImageId(Long imageId) {
        return metadataMapper.findByImageId(imageId);
    }

    @Override
    public int deleteByImageId(Long imageId) {
        return metadataMapper.deleteByImageId(imageId);
    }
}
