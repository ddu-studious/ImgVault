package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 秒传请求 DTO
 */
@Data
@Schema(description = "秒传请求")
public class InstantUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件 SHA-256 哈希", required = true)
    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    @Schema(description = "文件 MD5 哈希", required = true)
    @NotBlank(message = "MD5 不能为空")
    private String fileMd5;

    @Schema(description = "文件大小(字节)", required = true)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @Schema(description = "原始文件名")
    private String originalName;
}
