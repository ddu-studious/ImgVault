package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 预签名直传请求
 */
@Data
@Schema(description = "预签名直传请求")
public class PresignedUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件名", required = true)
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "内容类型 (如 image/jpeg)")
    private String contentType;
}
