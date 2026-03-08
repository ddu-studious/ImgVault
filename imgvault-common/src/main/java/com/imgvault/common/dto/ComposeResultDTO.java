package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "图片合成结果")
public class ComposeResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "图片ID")
    private Long imageId;

    @Schema(description = "图片UUID")
    private String imageUuid;

    @Schema(description = "下载URL")
    private String downloadUrl;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "格式")
    private String format;
}
