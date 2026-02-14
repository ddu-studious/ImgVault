package com.imgvault.api.controller;

import com.imgvault.app.service.AlbumAppService;
import com.imgvault.common.dto.PageResult;
import com.imgvault.common.dto.Result;
import com.imgvault.domain.entity.AlbumEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * F25: 相册管理 API
 */
@RestController
@RequestMapping("/api/v1/albums")
@RequiredArgsConstructor
@Tag(name = "相册管理", description = "相册 CRUD + 图片管理")
public class AlbumController {

    private final AlbumAppService albumAppService;

    @PostMapping
    @Operation(summary = "创建相册")
    public Result<AlbumEntity> create(@RequestBody Map<String, String> body) {
        return Result.success("创建成功",
                albumAppService.createAlbum(body.get("name"), body.get("description")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取相册详情")
    public Result<AlbumEntity> getById(@PathVariable Long id) {
        return Result.success(albumAppService.getAlbum(id));
    }

    @GetMapping
    @Operation(summary = "相册列表")
    public Result<PageResult<AlbumEntity>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(albumAppService.listAlbums(page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新相册")
    public Result<AlbumEntity> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success("更新成功",
                albumAppService.updateAlbum(id, body.get("name"), body.get("description")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除相册")
    public Result<Void> delete(@PathVariable Long id) {
        albumAppService.deleteAlbum(id);
        return Result.success("删除成功", null);
    }

    /**
     * 添加图片到相册
     */
    @PostMapping("/{albumId}/images/{imageId}")
    @Operation(summary = "添加图片到相册")
    public Result<Void> addImage(
            @Parameter(description = "相册ID") @PathVariable Long albumId,
            @Parameter(description = "图片ID") @PathVariable Long imageId) {
        albumAppService.addImageToAlbum(albumId, imageId);
        return Result.success("图片已添加", null);
    }

    /**
     * 从相册移除图片
     */
    @DeleteMapping("/{albumId}/images/{imageId}")
    @Operation(summary = "从相册移除图片")
    public Result<Void> removeImage(
            @PathVariable Long albumId, @PathVariable Long imageId) {
        albumAppService.removeImageFromAlbum(albumId, imageId);
        return Result.success("图片已移除", null);
    }

    /**
     * 设置相册封面
     */
    @PutMapping("/{albumId}/cover/{imageId}")
    @Operation(summary = "设置相册封面")
    public Result<Void> setCover(@PathVariable Long albumId, @PathVariable Long imageId) {
        albumAppService.setAlbumCover(albumId, imageId);
        return Result.success("封面已设置", null);
    }

    /**
     * 获取相册中的图片
     */
    @GetMapping("/{albumId}/images")
    @Operation(summary = "获取相册中的图片")
    public Result<PageResult<Long>> getAlbumImages(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(albumAppService.getAlbumImages(albumId, page, size));
    }
}
