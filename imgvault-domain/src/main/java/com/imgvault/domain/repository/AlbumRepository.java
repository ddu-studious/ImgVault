package com.imgvault.domain.repository;

import com.imgvault.domain.entity.AlbumEntity;

import java.util.List;

/**
 * 相册仓储接口
 */
public interface AlbumRepository {

    void insert(AlbumEntity entity);

    AlbumEntity findById(Long id);

    List<AlbumEntity> findAll(int offset, int limit);

    int countAll();

    void update(AlbumEntity entity);

    void deleteById(Long id);

    void addImage(Long albumId, Long imageId, Integer sortOrder);

    void removeImage(Long albumId, Long imageId);

    void removeAllImages(Long albumId);

    List<Long> findImageIdsByAlbumId(Long albumId, int offset, int limit);

    int countImagesByAlbumId(Long albumId);

    void incrementImageCount(Long albumId);

    void decrementImageCount(Long albumId);
}
