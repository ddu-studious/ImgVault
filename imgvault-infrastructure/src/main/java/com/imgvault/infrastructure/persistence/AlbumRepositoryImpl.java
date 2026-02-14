package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.AlbumEntity;
import com.imgvault.domain.repository.AlbumRepository;
import com.imgvault.infrastructure.persistence.mapper.AlbumMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AlbumRepositoryImpl implements AlbumRepository {

    private final AlbumMapper albumMapper;

    @Override
    public void insert(AlbumEntity entity) {
        albumMapper.insert(entity);
    }

    @Override
    public AlbumEntity findById(Long id) {
        return albumMapper.findById(id);
    }

    @Override
    public List<AlbumEntity> findAll(int offset, int limit) {
        return albumMapper.findAll(offset, limit);
    }

    @Override
    public int countAll() {
        return albumMapper.countAll();
    }

    @Override
    public void update(AlbumEntity entity) {
        albumMapper.update(entity);
    }

    @Override
    public void deleteById(Long id) {
        albumMapper.deleteById(id);
    }

    @Override
    public void addImage(Long albumId, Long imageId, Integer sortOrder) {
        albumMapper.addImage(albumId, imageId, sortOrder);
    }

    @Override
    public void removeImage(Long albumId, Long imageId) {
        albumMapper.removeImage(albumId, imageId);
    }

    @Override
    public void removeAllImages(Long albumId) {
        albumMapper.removeAllImages(albumId);
    }

    @Override
    public List<Long> findImageIdsByAlbumId(Long albumId, int offset, int limit) {
        return albumMapper.findImageIdsByAlbumId(albumId, offset, limit);
    }

    @Override
    public int countImagesByAlbumId(Long albumId) {
        return albumMapper.countImagesByAlbumId(albumId);
    }

    @Override
    public void incrementImageCount(Long albumId) {
        albumMapper.incrementImageCount(albumId);
    }

    @Override
    public void decrementImageCount(Long albumId) {
        albumMapper.decrementImageCount(albumId);
    }
}
