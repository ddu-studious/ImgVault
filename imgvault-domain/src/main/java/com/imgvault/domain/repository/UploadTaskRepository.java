package com.imgvault.domain.repository;

import com.imgvault.domain.entity.UploadTaskEntity;

import java.util.List;

/**
 * 分片上传任务仓储接口
 */
public interface UploadTaskRepository {

    void insert(UploadTaskEntity entity);

    UploadTaskEntity findByUploadId(String uploadId);

    void update(UploadTaskEntity entity);

    void updateStatus(String uploadId, String status);

    List<UploadTaskEntity> findExpiredTasks(String expireTime);

    void deleteByUploadId(String uploadId);
}
