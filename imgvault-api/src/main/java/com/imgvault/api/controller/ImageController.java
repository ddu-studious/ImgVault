package com.imgvault.api.controller;

import com.imgvault.app.service.ImageAppService;
import com.imgvault.common.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.Valid;
import java.util.List;

/**
 * 图片 API 控制器
 * 提供图片上传/下载/查询/删除等 RESTful API
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "图片管理", description = "图片上传/下载/查询/删除接口")
public class ImageController {

    private final ImageAppService imageAppService;

    /**
     * F02: 单张图片上传
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片", description = "支持 JPEG/PNG/GIF/WebP/BMP 格式，最大 50MB")
    public Result<ImageUploadDTO> upload(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        return Result.success("上传成功", imageAppService.uploadImage(file, visitorId));
    }

    /**
     * F02: 批量图片上传
     */
    @PostMapping(value = "/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量上传图片", description = "支持同时上传多张图片")
    public Result<List<ImageUploadDTO>> batchUpload(
            @Parameter(description = "图片文件列表") @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        return Result.success("批量上传完成", imageAppService.batchUpload(files, visitorId));
    }

    /**
     * F04: 按 ID 查询图片详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询图片详情(按ID)")
    public Result<ImageDetailDTO> getById(
            @Parameter(description = "图片ID") @PathVariable Long id) {
        return Result.success(imageAppService.getImageById(id));
    }

    /**
     * F04: 按 UUID 查询图片详情
     */
    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "查询图片详情(按UUID)")
    public Result<ImageDetailDTO> getByUuid(
            @Parameter(description = "图片UUID") @PathVariable String uuid) {
        return Result.success(imageAppService.getImageByUuid(uuid));
    }

    /**
     * F05: 分页查询图片列表
     */
    @GetMapping
    @Operation(summary = "分页查询图片列表", description = "支持按格式/时间/大小排序筛选")
    public Result<PageResult<ImageDetailDTO>> list(
            @Valid ImageQueryRequest request,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        request.setVisitorId(visitorId);
        return Result.success(imageAppService.listImages(request));
    }

    /**
     * F06: 下载图片（302 重定向到预签名 URL）
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "下载图片", description = "返回 302 重定向到 MinIO 预签名 URL")
    public RedirectView download(
            @Parameter(description = "图片ID") @PathVariable Long id) {
        String url = imageAppService.getDownloadUrl(id);
        RedirectView redirectView = new RedirectView(url);
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    /**
     * F06: 获取下载 URL（不重定向）
     */
    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取下载URL", description = "返回预签名下载 URL，有效期 1 小时")
    public Result<String> getDownloadUrl(
            @Parameter(description = "图片ID") @PathVariable Long id) {
        return Result.success(imageAppService.getDownloadUrl(id));
    }

    /**
     * F07: 软删除图片
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片(软删除)", description = "标记为已删除，可恢复")
    public Result<Void> softDelete(
            @Parameter(description = "图片ID") @PathVariable Long id) {
        imageAppService.softDeleteImage(id);
        return Result.success("删除成功", null);
    }

    /**
     * F07: 物理删除图片
     */
    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "永久删除图片", description = "物理删除，同时删除 MinIO 文件，不可恢复")
    public Result<Void> hardDelete(
            @Parameter(description = "图片ID") @PathVariable Long id) {
        imageAppService.hardDeleteImage(id);
        return Result.success("永久删除成功", null);
    }

    // ==================== Phase 3: 高级上传功能 ====================

    /**
     * F18: 秒传检测
     */
    @PostMapping("/instant-upload")
    @Operation(summary = "秒传检测", description = "通过文件哈希检测是否已存在，实现秒传")
    public Result<InstantUploadDTO> instantUpload(@Valid @RequestBody InstantUploadRequest request) {
        return Result.success(imageAppService.checkInstantUpload(request));
    }

    /**
     * F19: 获取预签名上传 URL
     */
    @PostMapping("/presigned-upload")
    @Operation(summary = "获取预签名上传URL", description = "客户端直传 MinIO，返回预签名 PUT URL")
    public Result<PresignedUploadDTO> getPresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        return Result.success(imageAppService.getPresignedUploadUrl(request));
    }

    /**
     * F19: 预签名上传确认
     */
    @PostMapping("/presigned-upload/confirm")
    @Operation(summary = "预签名上传确认", description = "客户端直传完成后，调用此接口创建图片记录")
    public Result<ImageUploadDTO> confirmPresignedUpload(
            @Valid @RequestBody PresignedUploadConfirmRequest request) {
        return Result.success("上传确认成功", imageAppService.confirmPresignedUpload(request));
    }

    /**
     * F20: 初始化分片上传
     */
    @PostMapping("/chunk-upload/init")
    @Operation(summary = "初始化分片上传", description = "返回上传任务ID和分片信息")
    public Result<ChunkUploadInitDTO> initChunkUpload(
            @Valid @RequestBody ChunkUploadInitRequest request) {
        return Result.success(imageAppService.initChunkUpload(request));
    }

    /**
     * F20 + F21: 上传分片
     */
    @PostMapping(value = "/chunk-upload/{uploadId}/{chunkNumber}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传分片", description = "上传单个分片，支持断点续传")
    public Result<ChunkUploadDTO> uploadChunk(
            @Parameter(description = "上传任务ID") @PathVariable String uploadId,
            @Parameter(description = "分片编号(从1开始)") @PathVariable int chunkNumber,
            @Parameter(description = "分片数据") @RequestParam("chunk") MultipartFile chunkFile) {
        return Result.success(imageAppService.uploadChunk(uploadId, chunkNumber, chunkFile));
    }

    /**
     * F21: 查询上传进度
     */
    @GetMapping("/chunk-upload/{uploadId}/progress")
    @Operation(summary = "查询上传进度", description = "返回已上传分片列表，用于断点续传")
    public Result<ChunkUploadInitDTO> getUploadProgress(
            @Parameter(description = "上传任务ID") @PathVariable String uploadId) {
        return Result.success(imageAppService.getUploadProgress(uploadId));
    }

    // ==================== Phase 2: imgproxy 图片处理 ====================

    /**
     * F12-F17: 图片处理（302 重定向到 imgproxy URL）
     * Java 服务生成签名后的 imgproxy URL，客户端直接访问 imgproxy 获取处理后的图片
     */
    @GetMapping("/{id}/process")
    @Operation(summary = "图片处理", description = "支持缩放/格式转换/压缩/智能裁剪，302 重定向到 imgproxy")
    public RedirectView process(
            @Parameter(description = "图片ID") @PathVariable Long id,
            @Valid ImageProcessRequest request) {
        String url = imageAppService.getProcessedImageUrl(id, request);
        RedirectView redirectView = new RedirectView(url);
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }

    /**
     * F12-F17: 获取图片处理 URL（不重定向）
     */
    @GetMapping("/{id}/process-url")
    @Operation(summary = "获取图片处理URL", description = "返回签名后的 imgproxy URL，不重定向")
    public Result<String> getProcessUrl(
            @Parameter(description = "图片ID") @PathVariable Long id,
            @Valid ImageProcessRequest request) {
        String url = imageAppService.getProcessedImageUrl(id, request);
        return Result.success(url);
    }
}
