package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.AlbumEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlbumMapper {

    void insert(AlbumEntity entity);

    AlbumEntity findById(@Param("id") Long id);

    List<AlbumEntity> findAll(@Param("offset") int offset, @Param("limit") int limit);

    int countAll();

    void update(AlbumEntity entity);

    void deleteById(@Param("id") Long id);

    void addImage(@Param("albumId") Long albumId, @Param("imageId") Long imageId,
                  @Param("sortOrder") Integer sortOrder);

    void removeImage(@Param("albumId") Long albumId, @Param("imageId") Long imageId);

    void removeAllImages(@Param("albumId") Long albumId);

    List<Long> findImageIdsByAlbumId(@Param("albumId") Long albumId,
                                      @Param("offset") int offset, @Param("limit") int limit);

    int countImagesByAlbumId(@Param("albumId") Long albumId);

    void incrementImageCount(@Param("albumId") Long albumId);

    void decrementImageCount(@Param("albumId") Long albumId);
}
