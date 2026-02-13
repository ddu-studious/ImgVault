package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 图片处理请求参数
 */
@Data
@Schema(description = "图片处理参数")
public class ImageProcessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目标宽度", example = "400")
    @Min(value = 0, message = "宽度不能为负数")
    @Max(value = 10000, message = "宽度不能超过10000")
    private int width;

    @Schema(description = "目标高度", example = "300")
    @Min(value = 0, message = "高度不能为负数")
    @Max(value = 10000, message = "高度不能超过10000")
    private int height;

    @Schema(description = "输出格式: jpeg/png/webp/avif", example = "webp")
    private String format;

    @Schema(description = "压缩质量 1-100", example = "85", defaultValue = "0")
    @Min(value = 0, message = "质量最小为0")
    @Max(value = 100, message = "质量最大为100")
    private int quality;

    @Schema(description = "是否智能裁剪", example = "false", defaultValue = "false")
    private boolean smartCrop;
}
