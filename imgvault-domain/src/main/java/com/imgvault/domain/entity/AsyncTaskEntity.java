package com.imgvault.domain.entity;

import lombok.Data;

/**
 * 异步任务实体
 */
@Data
public class AsyncTaskEntity {

    private Long id;
    private String taskType;
    private Long imageId;
    private String params;
    private String status;
    private Integer retryCount;
    private Integer maxRetry;
    private String errorMessage;
    private String createdAt;
    private String updatedAt;
    private String executedAt;
}
