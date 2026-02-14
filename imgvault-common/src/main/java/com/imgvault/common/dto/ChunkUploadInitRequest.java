package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分片上传初始化请求
 */
@Data
@Schema(description = "分片上传初始化请求")
public class ChunkUploadInitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件名", required = true)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件大小(字节)", required = true)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @Schema(description = "文件 SHA-256 哈希（可选，用于秒传检测）")
    private String fileHash;

    @Schema(description = "分片大小(字节)，默认 5MB")
    private Integer chunkSize;
}
