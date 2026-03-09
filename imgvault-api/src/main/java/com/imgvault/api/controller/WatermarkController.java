package com.imgvault.api.controller;

import com.imgvault.app.service.WatermarkAppService;
import com.imgvault.common.dto.Result;
import com.imgvault.common.dto.WatermarkRemoveResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 水印去除 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/watermark")
@RequiredArgsConstructor
@Tag(name = "水印去除", description = "基于遮罩的水印去除接口")
public class WatermarkController {

    private final WatermarkAppService watermarkAppService;

    @PostMapping(value = "/remove", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "去除水印", description = "根据遮罩去除图片水印，支持 opencv 和 iopaint 两种引擎")
    public Result<WatermarkRemoveResultDTO> remove(
            @Parameter(description = "已有图片ID（与file二选一）") @RequestParam(value = "imageId", required = false) Long imageId,
            @Parameter(description = "上传的图片文件（与imageId二选一）") @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(description = "Base64 编码的遮罩 PNG，白色区域为水印") @RequestParam("maskData") String maskData,
            @Parameter(description = "引擎: opencv / iopaint") @RequestParam(value = "engine", defaultValue = "opencv") String engine,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        WatermarkRemoveResultDTO result = watermarkAppService.removeWatermark(imageId, file, maskData, engine, visitorId);
        return Result.success(result);
    }
}
