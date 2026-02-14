package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.UploadTaskEntity;
import com.imgvault.domain.repository.UploadTaskRepository;
import com.imgvault.infrastructure.persistence.mapper.UploadTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UploadTaskRepositoryImpl implements UploadTaskRepository {

    private final UploadTaskMapper uploadTaskMapper;

    @Override
    public void insert(UploadTaskEntity entity) {
        uploadTaskMapper.insert(entity);
    }

    @Override
    public UploadTaskEntity findByUploadId(String uploadId) {
        return uploadTaskMapper.findByUploadId(uploadId);
    }

    @Override
    public void update(UploadTaskEntity entity) {
        uploadTaskMapper.update(entity);
    }

    @Override
    public void updateStatus(String uploadId, String status) {
        uploadTaskMapper.updateStatus(uploadId, status);
    }

    @Override
    public List<UploadTaskEntity> findExpiredTasks(String expireTime) {
        return uploadTaskMapper.findExpiredTasks(expireTime);
    }

    @Override
    public void deleteByUploadId(String uploadId) {
        uploadTaskMapper.deleteByUploadId(uploadId);
    }
}
