package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.OperationLogEntity;
import com.imgvault.domain.repository.OperationLogRepository;
import com.imgvault.infrastructure.persistence.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void insert(OperationLogEntity entity) {
        operationLogMapper.insert(entity);
    }

    @Override
    public List<OperationLogEntity> findByTargetId(String targetType, Long targetId, int offset, int limit) {
        return operationLogMapper.findByTargetId(targetType, targetId, offset, limit);
    }

    @Override
    public List<OperationLogEntity> findRecent(int limit) {
        return operationLogMapper.findRecent(limit);
    }

    @Override
    public int countAll() {
        return operationLogMapper.countAll();
    }
}
