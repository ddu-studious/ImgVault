package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分片上传结果
 */
@Data
@Schema(description = "分片上传结果")
public class ChunkUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否上传成功")
    private boolean success;

    @Schema(description = "当前分片编号")
    private int chunkNumber;

    @Schema(description = "是否全部上传完成")
    private boolean allUploaded;

    @Schema(description = "图片ID（全部完成时返回）")
    private Long imageId;

    @Schema(description = "图片UUID（全部完成时返回）")
    private String imageUuid;
}
