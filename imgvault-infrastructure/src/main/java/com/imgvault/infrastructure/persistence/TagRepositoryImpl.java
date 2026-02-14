package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.TagEntity;
import com.imgvault.domain.repository.TagRepository;
import com.imgvault.infrastructure.persistence.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

    private final TagMapper tagMapper;

    @Override
    public void insert(TagEntity entity) {
        tagMapper.insert(entity);
    }

    @Override
    public TagEntity findById(Long id) {
        return tagMapper.findById(id);
    }

    @Override
    public TagEntity findByName(String name) {
        return tagMapper.findByName(name);
    }

    @Override
    public List<TagEntity> findAll() {
        return tagMapper.findAll();
    }

    @Override
    public List<TagEntity> findByImageId(Long imageId) {
        return tagMapper.findByImageId(imageId);
    }

    @Override
    public void update(TagEntity entity) {
        tagMapper.update(entity);
    }

    @Override
    public void deleteById(Long id) {
        tagMapper.deleteById(id);
    }

    @Override
    public void addImageTag(Long imageId, Long tagId) {
        tagMapper.addImageTag(imageId, tagId);
    }

    @Override
    public void removeImageTag(Long imageId, Long tagId) {
        tagMapper.removeImageTag(imageId, tagId);
    }

    @Override
    public void removeAllImageTags(Long imageId) {
        tagMapper.removeAllImageTags(imageId);
    }

    @Override
    public List<Long> findImageIdsByTagId(Long tagId, int offset, int limit) {
        return tagMapper.findImageIdsByTagId(tagId, offset, limit);
    }

    @Override
    public int countImagesByTagId(Long tagId) {
        return tagMapper.countImagesByTagId(tagId);
    }

    @Override
    public void incrementImageCount(Long tagId) {
        tagMapper.incrementImageCount(tagId);
    }

    @Override
    public void decrementImageCount(Long tagId) {
        tagMapper.decrementImageCount(tagId);
    }
}
