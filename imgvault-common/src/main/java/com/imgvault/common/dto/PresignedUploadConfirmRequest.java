package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 预签名直传确认请求
 */
@Data
@Schema(description = "预签名直传完成后的确认请求")
public class PresignedUploadConfirmRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "存储路径", required = true)
    @NotBlank(message = "存储路径不能为空")
    private String storagePath;

    @Schema(description = "文件大小(字节)", required = true)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件 SHA-256 哈希（客户端计算）")
    private String fileHash;

    @Schema(description = "文件 MD5 哈希（客户端计算）")
    private String fileMd5;
}
