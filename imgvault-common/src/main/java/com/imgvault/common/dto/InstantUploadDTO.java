package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 秒传结果 DTO
 */
@Data
@Schema(description = "秒传结果")
public class InstantUploadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否秒传成功")
    private boolean matched;

    @Schema(description = "图片ID（秒传成功时返回）")
    private Long imageId;

    @Schema(description = "图片UUID（秒传成功时返回）")
    private String imageUuid;

    @Schema(description = "下载URL（秒传成功时返回）")
    private String downloadUrl;

    public static InstantUploadDTO notMatched() {
        InstantUploadDTO dto = new InstantUploadDTO();
        dto.setMatched(false);
        return dto;
    }

    public static InstantUploadDTO matched(Long imageId, String imageUuid, String downloadUrl) {
        InstantUploadDTO dto = new InstantUploadDTO();
        dto.setMatched(true);
        dto.setImageId(imageId);
        dto.setImageUuid(imageUuid);
        dto.setDownloadUrl(downloadUrl);
        return dto;
    }
}
