package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图片查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "图片分页查询请求")
public class ImageQueryRequest extends PageQuery {

    @Schema(description = "格式过滤: jpeg/png/gif/webp/bmp", example = "jpeg")
    private String format;

    @Schema(description = "状态过滤: 0-删除 1-正常 2-审核中（null=仅正常）", example = "1")
    private Integer status;

    @Schema(description = "关键词搜索（文件名模糊匹配）", example = "sunset")
    private String keyword;

    @Schema(description = "访客ID（前端自动传递，无需手动设置）", hidden = true)
    private String visitorId;
}
