package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.repository.ImageRepository;
import com.imgvault.infrastructure.persistence.mapper.ImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 图片仓库实现
 */
@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepository {

    private final ImageMapper imageMapper;

    @Override
    public int insert(ImageEntity entity) {
        return imageMapper.insert(entity);
    }

    @Override
    public ImageEntity findById(Long id) {
        return imageMapper.findById(id);
    }

    @Override
    public ImageEntity findByUuid(String imageUuid) {
        return imageMapper.findByUuid(imageUuid);
    }

    @Override
    public ImageEntity findByHash(String fileHash) {
        return imageMapper.findByHash(fileHash);
    }

    @Override
    public List<ImageEntity> findPage(String format, Integer status, String sortBy, String sortOrder, int offset, int limit) {
        return imageMapper.findPage(format, status, sortBy, sortOrder, offset, limit);
    }

    @Override
    public long count(String format, Integer status) {
        return imageMapper.count(format, status);
    }

    @Override
    public int updateStatus(Long id, Integer status) {
        return imageMapper.updateStatus(id, status);
    }

    @Override
    public int softDelete(Long id) {
        return imageMapper.softDelete(id);
    }

    @Override
    public int deleteById(Long id) {
        return imageMapper.deleteById(id);
    }

    @Override
    public int updateDescription(Long id, String description) {
        return imageMapper.updateDescription(id, description);
    }

    @Override
    public int incrementViewCount(Long id) {
        return imageMapper.incrementViewCount(id);
    }
}
