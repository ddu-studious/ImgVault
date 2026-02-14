package com.imgvault.domain.repository;

import com.imgvault.domain.entity.OperationLogEntity;

import java.util.List;

/**
 * 操作日志仓储接口
 */
public interface OperationLogRepository {

    void insert(OperationLogEntity entity);

    List<OperationLogEntity> findByTargetId(String targetType, Long targetId, int offset, int limit);

    List<OperationLogEntity> findRecent(int limit);

    int countAll();
}
