package com.imgvault.infrastructure.persistence;

import com.imgvault.domain.entity.FileFingerprintEntity;
import com.imgvault.domain.repository.FileFingerprintRepository;
import com.imgvault.infrastructure.persistence.mapper.FileFingerprintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 文件指纹仓库实现
 */
@Repository
@RequiredArgsConstructor
public class FileFingerprintRepositoryImpl implements FileFingerprintRepository {

    private final FileFingerprintMapper fingerprintMapper;

    @Override
    public FileFingerprintEntity findByHash(String fileHash) {
        return fingerprintMapper.findByHash(fileHash);
    }

    @Override
    public FileFingerprintEntity findByMd5(String fileMd5) {
        return fingerprintMapper.findByMd5(fileMd5);
    }

    @Override
    public int insert(FileFingerprintEntity entity) {
        return fingerprintMapper.insert(entity);
    }

    @Override
    public int incrementRefCount(Long id) {
        return fingerprintMapper.incrementRefCount(id);
    }

    @Override
    public int decrementRefCount(Long id) {
        return fingerprintMapper.decrementRefCount(id);
    }
}
