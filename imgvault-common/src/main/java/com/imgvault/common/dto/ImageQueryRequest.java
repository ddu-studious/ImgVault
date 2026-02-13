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
}
