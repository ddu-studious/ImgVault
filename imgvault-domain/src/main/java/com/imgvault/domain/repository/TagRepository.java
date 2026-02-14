package com.imgvault.domain.repository;

import com.imgvault.domain.entity.TagEntity;

import java.util.List;

/**
 * 标签仓储接口
 */
public interface TagRepository {

    void insert(TagEntity entity);

    TagEntity findById(Long id);

    TagEntity findByName(String name);

    List<TagEntity> findAll();

    List<TagEntity> findByImageId(Long imageId);

    void update(TagEntity entity);

    void deleteById(Long id);

    void addImageTag(Long imageId, Long tagId);

    void removeImageTag(Long imageId, Long tagId);

    void removeAllImageTags(Long imageId);

    List<Long> findImageIdsByTagId(Long tagId, int offset, int limit);

    int countImagesByTagId(Long tagId);

    void incrementImageCount(Long tagId);

    void decrementImageCount(Long tagId);
}
