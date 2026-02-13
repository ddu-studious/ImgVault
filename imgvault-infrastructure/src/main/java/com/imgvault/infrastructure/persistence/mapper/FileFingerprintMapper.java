package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.FileFingerprintEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件指纹 MyBatis Mapper
 */
@Mapper
public interface FileFingerprintMapper {

    FileFingerprintEntity findByHash(@Param("fileHash") String fileHash);

    FileFingerprintEntity findByMd5(@Param("fileMd5") String fileMd5);

    int insert(FileFingerprintEntity entity);

    int incrementRefCount(@Param("id") Long id);

    int decrementRefCount(@Param("id") Long id);
}
