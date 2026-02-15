package com.imgvault.app.service;

import com.imgvault.common.dto.PageResult;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.domain.entity.AlbumEntity;
import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.repository.AlbumRepository;
import com.imgvault.domain.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * F25: 相册管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumAppService {

    private final AlbumRepository albumRepository;
    private final ImageRepository imageRepository;

    /**
     * 创建相册
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public AlbumEntity createAlbum(String name, String description) {
        if (StringUtils.isBlank(name)) {
            throw BusinessException.badRequest("相册名称不能为空");
        }

        AlbumEntity entity = new AlbumEntity();
        entity.setName(name.trim());
        entity.setDescription(description);
        entity.setOwnerId(0L);  // 默认所有者（后续可替换为认证用户ID）
        entity.setImageCount(0);
        albumRepository.insert(entity);

        log.info("创建相册: id={}, name={}", entity.getId(), name);
        return entity;
    }

    /**
     * 获取相册详情
     */
    @Cacheable(value = "albumCache", key = "'id:' + #id")
    public AlbumEntity getAlbum(Long id) {
        AlbumEntity entity = albumRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("相册不存在: " + id);
        }
        return entity;
    }

    /**
     * 相册列表（分页）
     */
    public PageResult<AlbumEntity> listAlbums(int page, int size) {
        int total = albumRepository.countAll();
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<AlbumEntity> albums = albumRepository.findAll(offset, size);
        return PageResult.of(albums, total, page, size);
    }

    /**
     * 更新相册
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public AlbumEntity updateAlbum(Long id, String name, String description) {
        AlbumEntity entity = albumRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("相册不存在: " + id);
        }

        if (StringUtils.isNotBlank(name)) {
            entity.setName(name.trim());
        }
        if (description != null) {
            entity.setDescription(description);
        }
        albumRepository.update(entity);

        log.info("更新相册: id={}, name={}", id, entity.getName());
        return entity;
    }

    /**
     * 删除相册
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public void deleteAlbum(Long id) {
        AlbumEntity entity = albumRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("相册不存在: " + id);
        }

        albumRepository.removeAllImages(id);
        albumRepository.deleteById(id);
        log.info("删除相册: id={}, name={}", id, entity.getName());
    }

    /**
     * 添加图片到相册
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public void addImageToAlbum(Long albumId, Long imageId) {
        AlbumEntity album = albumRepository.findById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在: " + albumId);
        }
        ImageEntity image = imageRepository.findById(imageId);
        if (image == null) {
            throw BusinessException.notFound("图片不存在: " + imageId);
        }

        int currentCount = albumRepository.countImagesByAlbumId(albumId);
        albumRepository.addImage(albumId, imageId, currentCount + 1);
        albumRepository.incrementImageCount(albumId);

        // 如果是第一张图片，自动设为封面
        if (album.getCoverImageId() == null) {
            album.setCoverImageId(imageId);
            albumRepository.update(album);
        }

        log.info("添加图片到相册: albumId={}, imageId={}", albumId, imageId);
    }

    /**
     * 从相册移除图片
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public void removeImageFromAlbum(Long albumId, Long imageId) {
        albumRepository.removeImage(albumId, imageId);
        albumRepository.decrementImageCount(albumId);

        // 如果移除的是封面图片，重新设置封面
        AlbumEntity album = albumRepository.findById(albumId);
        if (album != null && imageId.equals(album.getCoverImageId())) {
            List<Long> remainingImages = albumRepository.findImageIdsByAlbumId(albumId, 0, 1);
            album.setCoverImageId(remainingImages.isEmpty() ? null : remainingImages.get(0));
            albumRepository.update(album);
        }

        log.info("从相册移除图片: albumId={}, imageId={}", albumId, imageId);
    }

    /**
     * 设置相册封面
     */
    @CacheEvict(value = "albumCache", allEntries = true)
    public void setAlbumCover(Long albumId, Long imageId) {
        AlbumEntity album = albumRepository.findById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在: " + albumId);
        }

        album.setCoverImageId(imageId);
        albumRepository.update(album);
        log.info("设置相册封面: albumId={}, coverImageId={}", albumId, imageId);
    }

    /**
     * 获取相册中的图片ID列表（分页）
     */
    public PageResult<Long> getAlbumImages(Long albumId, int page, int size) {
        AlbumEntity album = albumRepository.findById(albumId);
        if (album == null) {
            throw BusinessException.notFound("相册不存在: " + albumId);
        }

        int total = albumRepository.countImagesByAlbumId(albumId);
        if (total == 0) {
            return PageResult.empty(page, size);
        }
        int offset = (page - 1) * size;
        List<Long> imageIds = albumRepository.findImageIdsByAlbumId(albumId, offset, size);
        return PageResult.of(imageIds, total, page, size);
    }
}
