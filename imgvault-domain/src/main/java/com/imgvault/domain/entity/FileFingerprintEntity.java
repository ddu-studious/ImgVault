package com.imgvault.domain.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件指纹实体（秒传用）
 */
@Data
public class FileFingerprintEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileHash;
    private String fileMd5;
    private String storagePath;
    private Long fileSize;
    private Integer refCount;
    private String createdAt;
}
