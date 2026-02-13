package com.imgvault.domain.repository;

import com.imgvault.domain.entity.ImageEntity;

import java.util.List;

/**
 * 图片仓库接口
 */
public interface ImageRepository {

    /**
     * 插入图片记录
     */
    int insert(ImageEntity entity);

    /**
     * 根据 ID 查询
     */
    ImageEntity findById(Long id);

    /**
     * 根据 UUID 查询
     */
    ImageEntity findByUuid(String imageUuid);

    /**
     * 根据文件哈希查询
     */
    ImageEntity findByHash(String fileHash);

    /**
     * 分页查询图片列表
     *
     * @param format   格式过滤（可选）
     * @param status   状态过滤
     * @param sortBy   排序字段
     * @param sortOrder 排序方向
     * @param offset   偏移量
     * @param limit    每页大小
     * @return 图片列表
     */
    List<ImageEntity> findPage(String format, Integer status, String sortBy, String sortOrder, int offset, int limit);

    /**
     * 查询总数
     */
    long count(String format, Integer status);

    /**
     * 更新图片状态（软删除）
     */
    int updateStatus(Long id, Integer status);

    /**
     * 设置删除时间（软删除）
     */
    int softDelete(Long id);

    /**
     * 物理删除
     */
    int deleteById(Long id);

    /**
     * 更新图片描述
     */
    int updateDescription(Long id, String description);

    /**
     * 增加浏览次数
     */
    int incrementViewCount(Long id);
}
