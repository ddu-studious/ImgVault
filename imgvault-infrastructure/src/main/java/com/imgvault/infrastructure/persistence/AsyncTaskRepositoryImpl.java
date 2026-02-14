package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.AsyncTaskEntity;
import com.imgvault.domain.repository.AsyncTaskRepository;
import com.imgvault.infrastructure.persistence.mapper.AsyncTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AsyncTaskRepositoryImpl implements AsyncTaskRepository {

    private final AsyncTaskMapper asyncTaskMapper;

    @Override
    public void insert(AsyncTaskEntity entity) {
        asyncTaskMapper.insert(entity);
    }

    @Override
    public AsyncTaskEntity findById(Long id) {
        return asyncTaskMapper.findById(id);
    }

    @Override
    public List<AsyncTaskEntity> findPendingTasks(int limit) {
        return asyncTaskMapper.findPendingTasks(limit);
    }

    @Override
    public List<AsyncTaskEntity> findFailedRetryableTasks(int limit) {
        return asyncTaskMapper.findFailedRetryableTasks(limit);
    }

    @Override
    public void updateStatus(Long id, String status, String errorMessage) {
        asyncTaskMapper.updateStatus(id, status, errorMessage);
    }

    @Override
    public void incrementRetryCount(Long id) {
        asyncTaskMapper.incrementRetryCount(id);
    }

    @Override
    public void updateExecutedAt(Long id, String executedAt) {
        asyncTaskMapper.updateExecutedAt(id, executedAt);
    }

    @Override
    public int countByStatus(String status) {
        return asyncTaskMapper.countByStatus(status);
    }
}
