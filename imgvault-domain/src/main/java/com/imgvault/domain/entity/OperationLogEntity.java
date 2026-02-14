package com.imgvault.domain.entity;

import lombok.Data;

/**
 * 操作日志实体
 */
@Data
public class OperationLogEntity {

    private Long id;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String detail;
    private String operatorIp;
    private String createdAt;
}
