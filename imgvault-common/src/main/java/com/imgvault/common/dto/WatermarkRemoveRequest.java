package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 水印去除请求 DTO
 */
@Data
@Schema(description = "水印去除请求")
public class WatermarkRemoveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "已有图片ID（可选，与file二选一）")
    private Long imageId;

    @Schema(description = "Base64 编码的遮罩 PNG，白色区域为水印区域")
    private String maskData;

    @Schema(description = "引擎: opencv / iopaint", example = "opencv")
    private String engine = "opencv";
}
