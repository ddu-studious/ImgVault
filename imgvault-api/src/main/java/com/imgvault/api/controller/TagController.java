package com.imgvault.api.controller;

import com.imgvault.app.service.TagAppService;
import com.imgvault.common.dto.PageResult;
import com.imgvault.common.dto.Result;
import com.imgvault.domain.entity.TagEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * F24: 标签管理 API
 */
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "标签 CRUD + 图片关联")
public class TagController {

    private final TagAppService tagAppService;

    @PostMapping
    @Operation(summary = "创建标签")
    public Result<TagEntity> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        return Result.success("创建成功", tagAppService.createTag(name));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标签详情")
    public Result<TagEntity> getById(@PathVariable Long id) {
        return Result.success(tagAppService.getTag(id));
    }

    @GetMapping
    @Operation(summary = "获取所有标签")
    public Result<List<TagEntity>> listAll() {
        return Result.success(tagAppService.listAllTags());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    public Result<TagEntity> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        return Result.success("更新成功", tagAppService.updateTag(id, name));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public Result<Void> delete(@PathVariable Long id) {
        tagAppService.deleteTag(id);
        return Result.success("删除成功", null);
    }

    /**
     * 为图片添加标签
     */
    @PostMapping("/images/{imageId}/tags/{tagId}")
    @Operation(summary = "为图片添加标签")
    public Result<Void> addTagToImage(
            @Parameter(description = "图片ID") @PathVariable Long imageId,
            @Parameter(description = "标签ID") @PathVariable Long tagId) {
        tagAppService.addTagToImage(imageId, tagId);
        return Result.success("标签已添加", null);
    }

    /**
     * 为图片批量添加标签（通过名称）
     */
    @PostMapping("/images/{imageId}/tags")
    @Operation(summary = "为图片批量添加标签", description = "通过标签名称批量添加，自动创建不存在的标签")
    public Result<Void> addTagsToImage(
            @Parameter(description = "图片ID") @PathVariable Long imageId,
            @RequestBody List<String> tagNames) {
        tagAppService.addTagsToImage(imageId, tagNames);
        return Result.success("标签已批量添加", null);
    }

    /**
     * 移除图片标签
     */
    @DeleteMapping("/images/{imageId}/tags/{tagId}")
    @Operation(summary = "移除图片标签")
    public Result<Void> removeTagFromImage(
            @PathVariable Long imageId, @PathVariable Long tagId) {
        tagAppService.removeTagFromImage(imageId, tagId);
        return Result.success("标签已移除", null);
    }

    /**
     * 获取图片的所有标签
     */
    @GetMapping("/images/{imageId}/tags")
    @Operation(summary = "获取图片的所有标签")
    public Result<List<TagEntity>> getImageTags(@PathVariable Long imageId) {
        return Result.success(tagAppService.getImageTags(imageId));
    }

    /**
     * 按标签检索图片
     */
    @GetMapping("/{tagId}/images")
    @Operation(summary = "按标签检索图片", description = "返回关联此标签的图片ID列表")
    public Result<PageResult<Long>> findImagesByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(tagAppService.findImagesByTag(tagId, page, size));
    }
}
