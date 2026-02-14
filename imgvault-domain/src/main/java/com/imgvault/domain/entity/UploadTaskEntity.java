package com.imgvault.domain.entity;

import lombok.Data;

/**
 * 分片上传任务实体
 */
@Data
public class UploadTaskEntity {

    private Long id;
    private String uploadId;
    private String fileName;
    private Long fileSize;
    private String fileHash;
    private Integer chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunks;
    /** 逗号分隔的已上传分片编号 */
    private String uploadedParts;
    private String storagePath;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String expiresAt;
}
