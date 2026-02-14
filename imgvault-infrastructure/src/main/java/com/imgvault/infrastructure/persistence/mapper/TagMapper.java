package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.TagEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    void insert(TagEntity entity);

    TagEntity findById(@Param("id") Long id);

    TagEntity findByName(@Param("name") String name);

    List<TagEntity> findAll();

    List<TagEntity> findByImageId(@Param("imageId") Long imageId);

    void update(TagEntity entity);

    void deleteById(@Param("id") Long id);

    void addImageTag(@Param("imageId") Long imageId, @Param("tagId") Long tagId);

    void removeImageTag(@Param("imageId") Long imageId, @Param("tagId") Long tagId);

    void removeAllImageTags(@Param("imageId") Long imageId);

    List<Long> findImageIdsByTagId(@Param("tagId") Long tagId,
                                    @Param("offset") int offset, @Param("limit") int limit);

    int countImagesByTagId(@Param("tagId") Long tagId);

    void incrementImageCount(@Param("tagId") Long tagId);

    void decrementImageCount(@Param("tagId") Long tagId);
}
