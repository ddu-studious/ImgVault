package com.imgvault.infrastructure.persistence.mapper;

import com.imgvault.domain.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    void insert(OperationLogEntity entity);

    List<OperationLogEntity> findByTargetId(@Param("targetType") String targetType,
                                             @Param("targetId") Long targetId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    List<OperationLogEntity> findRecent(@Param("limit") int limit);

    int countAll();
}
