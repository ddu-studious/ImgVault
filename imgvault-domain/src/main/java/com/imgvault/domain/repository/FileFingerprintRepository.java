package com.imgvault.domain.repository;

import com.imgvault.domain.entity.FileFingerprintEntity;

/**
 * 文件指纹仓库接口
 */
public interface FileFingerprintRepository {

    /**
     * 根据文件哈希查询指纹
     */
    FileFingerprintEntity findByHash(String fileHash);

    /**
     * 根据 MD5 查询指纹
     */
    FileFingerprintEntity findByMd5(String fileMd5);

    /**
     * 插入指纹记录
     */
    int insert(FileFingerprintEntity entity);

    /**
     * 增加引用计数
     */
    int incrementRefCount(Long id);

    /**
     * 减少引用计数
     */
    int decrementRefCount(Long id);
}
