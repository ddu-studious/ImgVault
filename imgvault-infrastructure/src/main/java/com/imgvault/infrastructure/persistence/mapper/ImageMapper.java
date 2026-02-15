package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.ImageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片 MyBatis Mapper
 */
@Mapper
public interface ImageMapper {

    int insert(ImageEntity entity);

    ImageEntity findById(@Param("id") Long id);

    ImageEntity findByUuid(@Param("imageUuid") String imageUuid);

    ImageEntity findByHash(@Param("fileHash") String fileHash);

    List<ImageEntity> findPage(@Param("format") String format,
                               @Param("status") Integer status,
                               @Param("keyword") String keyword,
                               @Param("sortBy") String sortBy,
                               @Param("sortOrder") String sortOrder,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    long count(@Param("format") String format, @Param("status") Integer status, @Param("keyword") String keyword);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int softDelete(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    int updateDescription(@Param("id") Long id, @Param("description") String description);

    int incrementViewCount(@Param("id") Long id);

    // ===== Admin 统计查询 =====
    long countByStatus(@Param("status") Integer status);

    long sumFileSize();

    List<java.util.Map<String, Object>> countByFormat();

    long countTodayUploads();
}
