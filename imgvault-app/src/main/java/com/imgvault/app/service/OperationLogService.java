package com.imgvault.app.service;

import com.imgvault.domain.entity.OperationLogEntity;
import com.imgvault.domain.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * F29: 操作日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 异步记录操作日志
     */
    @Async("asyncTaskExecutor")
    public void logOperation(String operationType, String targetType,
                              Long targetId, String detail, String operatorIp) {
        try {
            OperationLogEntity entity = new OperationLogEntity();
            entity.setOperationType(operationType);
            entity.setTargetType(targetType);
            entity.setTargetId(targetId);
            entity.setDetail(detail);
            entity.setOperatorIp(operatorIp);
            operationLogRepository.insert(entity);
            log.debug("操作日志已记录: type={}, target={}:{}", operationType, targetType, targetId);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    /**
     * 查询目标对象的操作日志
     */
    public List<OperationLogEntity> getTargetLogs(String targetType, Long targetId,
                                                    int page, int size) {
        int offset = (page - 1) * size;
        return operationLogRepository.findByTargetId(targetType, targetId, offset, size);
    }

    /**
     * 查询最近操作日志
     */
    public List<OperationLogEntity> getRecentLogs(int limit) {
        return operationLogRepository.findRecent(limit);
    }

    /**
     * 获取总日志数
     */
    public int getTotalCount() {
        return operationLogRepository.countAll();
    }
}
