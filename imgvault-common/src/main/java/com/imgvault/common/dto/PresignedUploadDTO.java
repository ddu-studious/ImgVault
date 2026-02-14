package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 预签名直传 DTO
 */
@Data
@Schema(description = "预签名直传信息")
public class PresignedUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "预签名上传 URL")
    private String uploadUrl;

    @Schema(description = "存储路径")
    private String storagePath;

    @Schema(description = "有效期（秒）")
    private int expirySeconds;
}
