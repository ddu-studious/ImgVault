package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.UploadTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UploadTaskMapper {

    void insert(UploadTaskEntity entity);

    UploadTaskEntity findByUploadId(@Param("uploadId") String uploadId);

    void update(UploadTaskEntity entity);

    void updateStatus(@Param("uploadId") String uploadId, @Param("status") String status);

    List<UploadTaskEntity> findExpiredTasks(@Param("expireTime") String expireTime);

    void deleteByUploadId(@Param("uploadId") String uploadId);
}
