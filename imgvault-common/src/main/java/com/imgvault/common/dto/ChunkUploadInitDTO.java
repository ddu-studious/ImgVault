package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分片上传初始化结果
 */
@Data
@Schema(description = "分片上传初始化结果")
public class ChunkUploadInitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "上传任务 ID")
    private String uploadId;

    @Schema(description = "总分片数")
    private int totalChunks;

    @Schema(description = "分片大小(字节)")
    private int chunkSize;

    @Schema(description = "已上传的分片编号列表（断点续传用）")
    private List<Integer> uploadedChunks;
}
