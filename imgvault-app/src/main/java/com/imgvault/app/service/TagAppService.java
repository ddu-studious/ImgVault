package com.imgvault.app.service;

import com.imgvault.common.dto.PageResult;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.entity.TagEntity;
import com.imgvault.domain.repository.ImageRepository;
import com.imgvault.domain.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * F24: 标签管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagAppService {

    private final TagRepository tagRepository;
    private final ImageRepository imageRepository;

    /**
     * 创建标签
     */
    @CacheEvict(value = "tagCache", allEntries = true)
    public TagEntity createTag(String name) {
        if (StringUtils.isBlank(name)) {
            throw BusinessException.badRequest("标签名称不能为空");
        }
        name = name.trim();
        if (name.length() > 50) {
            throw BusinessException.badRequest("标签名称不能超过50个字符");
        }

        // 检查重复
        TagEntity existing = tagRepository.findByName(name);
        if (existing != null) {
            throw BusinessException.conflict("标签已存在: " + name);
        }

        TagEntity entity = new TagEntity();
        entity.setName(name);
        entity.setImageCount(0);
        tagRepository.insert(entity);
        log.info("创建标签: id={}, name={}", entity.getId(), name);
        return entity;
    }

    /**
     * 获取标签详情
     */
    @Cacheable(value = "tagCache", key = "'id:' + #id")
    public TagEntity getTag(Long id) {
        TagEntity entity = tagRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签不存在: " + id);
        }
        return entity;
    }

    /**
     * 获取所有标签
     */
    @Cacheable(value = "tagCache", key = "'all'")
    public List<TagEntity> listAllTags() {
        return tagRepository.findAll();
    }

    /**
     * 更新标签
     */
    @CacheEvict(value = "tagCache", allEntries = true)
    public TagEntity updateTag(Long id, String name) {
        TagEntity entity = tagRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签不存在: " + id);
        }

        if (StringUtils.isNotBlank(name)) {
            TagEntity existing = tagRepository.findByName(name.trim());
            if (existing != null && !existing.getId().equals(id)) {
                throw BusinessException.conflict("标签名已存在: " + name);
            }
            entity.setName(name.trim());
        }

        tagRepository.update(entity);
        log.info("更新标签: id={}, name={}", id, entity.getName());
        return entity;
    }

    /**
     * 删除标签
     */
    @CacheEvict(value = "tagCache", allEntries = true)
    public void deleteTag(Long id) {
        TagEntity entity = tagRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("标签不存在: " + id);
        }
        tagRepository.deleteById(id);
        log.info("删除标签: id={}, name={}", id, entity.getName());
    }

    /**
     * 为图片添加标签
     */
    @CacheEvict(value = {"tagCache", "imageCache"}, allEntries = true)
    public void addTagToImage(Long imageId, Long tagId) {
        // 验证图片存在
        ImageEntity image = imageRepository.findById(imageId);
        if (image == null) {
            throw BusinessException.notFound("图片不存在: " + imageId);
        }
        TagEntity tag = tagRepository.findById(tagId);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在: " + tagId);
        }

        tagRepository.addImageTag(imageId, tagId);
        tagRepository.incrementImageCount(tagId);
        log.info("为图片添加标签: imageId={}, tagId={}, tagName={}", imageId, tagId, tag.getName());
    }

    /**
     * 为图片批量添加标签（支持通过名称创建）
     */
    @CacheEvict(value = {"tagCache", "imageCache"}, allEntries = true)
    public void addTagsToImage(Long imageId, List<String> tagNames) {
        ImageEntity image = imageRepository.findById(imageId);
        if (image == null) {
            throw BusinessException.notFound("图片不存在: " + imageId);
        }

        for (String tagName : tagNames) {
            if (StringUtils.isBlank(tagName)) continue;
            tagName = tagName.trim();

            TagEntity tag = tagRepository.findByName(tagName);
            if (tag == null) {
                // 自动创建标签
                tag = new TagEntity();
                tag.setName(tagName);
                tag.setImageCount(0);
                tagRepository.insert(tag);
            }

            tagRepository.addImageTag(imageId, tag.getId());
            tagRepository.incrementImageCount(tag.getId());
        }
    }

    /**
     * 移除图片标签
     */
    @CacheEvict(value = {"tagCache", "imageCache"}, allEntries = true)
    public void removeTagFromImage(Long imageId, Long tagId) {
        tagRepository.removeImageTag(imageId, tagId);
        tagRepository.decrementImageCount(tagId);
        log.info("移除图片标签: imageId={}, tagId={}", imageId, tagId);
    }

    /**
     * 获取图片的所有标签
     */
    public List<TagEntity> getImageTags(Long imageId) {
        return tagRepository.findByImageId(imageId);
    }

    /**
     * 按标签检索图片（分页）
     */
    public PageResult<Long> findImagesByTag(Long tagId, int page, int size) {
        int total = tagRepository.countImagesByTagId(tagId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<Long> imageIds = tagRepository.findImageIdsByTagId(tagId, offset, size);
        return PageResult.of(imageIds, total, page, size);
    }
}
