package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.AsyncTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AsyncTaskMapper {

    void insert(AsyncTaskEntity entity);

    AsyncTaskEntity findById(@Param("id") Long id);

    List<AsyncTaskEntity> findPendingTasks(@Param("limit") int limit);

    List<AsyncTaskEntity> findFailedRetryableTasks(@Param("limit") int limit);

    void updateStatus(@Param("id") Long id, @Param("status") String status,
                      @Param("errorMessage") String errorMessage);

    void incrementRetryCount(@Param("id") Long id);

    void updateExecutedAt(@Param("id") Long id, @Param("executedAt") String executedAt);

    int countByStatus(@Param("status") String status);
}
