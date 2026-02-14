package com.imgvault.domain.repository;

import com.imgvault.domain.entity.AsyncTaskEntity;

import java.util.List;

/**
 * 异步任务仓储接口
 */
public interface AsyncTaskRepository {

    void insert(AsyncTaskEntity entity);

    AsyncTaskEntity findById(Long id);

    List<AsyncTaskEntity> findPendingTasks(int limit);

    List<AsyncTaskEntity> findFailedRetryableTasks(int limit);

    void updateStatus(Long id, String status, String errorMessage);

    void incrementRetryCount(Long id);

    void updateExecutedAt(Long id, String executedAt);

    int countByStatus(String status);
}
