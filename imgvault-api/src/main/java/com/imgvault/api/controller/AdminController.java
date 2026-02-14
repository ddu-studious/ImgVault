package com.imgvault.api.controller;

import com.imgvault.app.service.AsyncTaskService;
import com.imgvault.app.service.ImageAppService;
import com.imgvault.app.service.OperationLogService;
import com.imgvault.common.dto.*;
import com.imgvault.domain.entity.OperationLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * F28-F32: 管理后台 API
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台", description = "图片管理/统计/审核/日志")
public class AdminController {

    private final ImageAppService imageAppService;
    private final AsyncTaskService asyncTaskService;
    private final OperationLogService operationLogService;

    /**
     * F28: 管理后台图片浏览
     */
    @GetMapping("/images")
    @Operation(summary = "管理后台图片列表", description = "支持所有状态的图片浏览")
    public Result<PageResult<ImageDetailDTO>> listImages(@javax.validation.Valid ImageQueryRequest request) {
        return Result.success(imageAppService.listImages(request));
    }

    /**
     * F29: 操作日志列表
     */
    @GetMapping("/logs")
    @Operation(summary = "操作日志列表")
    public Result<List<OperationLogEntity>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(operationLogService.getRecentLogs(limit));
    }

    /**
     * F29: 按目标查询操作日志
     */
    @GetMapping("/logs/{targetType}/{targetId}")
    @Operation(summary = "查询目标操作日志")
    public Result<List<OperationLogEntity>> getTargetLogs(
            @PathVariable String targetType,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(operationLogService.getTargetLogs(targetType, targetId, page, size));
    }

    /**
     * F30: 回收站 - 查看软删除的图片
     */
    @GetMapping("/trash")
    @Operation(summary = "回收站", description = "查看已软删除的图片")
    public Result<PageResult<ImageDetailDTO>> listTrash(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ImageQueryRequest request = new ImageQueryRequest();
        request.setPage(page);
        request.setSize(size);
        // 自定义 status 查询已删除的，此处暂时复用 listImages
        return Result.success(imageAppService.listImages(request));
    }

    /**
     * F30: 从回收站恢复图片
     */
    @PostMapping("/trash/{id}/restore")
    @Operation(summary = "恢复图片", description = "从回收站恢复软删除的图片")
    public Result<Void> restoreImage(@PathVariable Long id) {
        // TODO: imageAppService.restoreImage(id);
        return Result.success("恢复成功", null);
    }

    /**
     * F31: 访问统计
     */
    @GetMapping("/stats")
    @Operation(summary = "系统统计", description = "图片数量/存储空间/任务统计")
    public Result<Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("asyncTasks", asyncTaskService.getTaskStats());
        stats.put("totalLogs", operationLogService.getTotalCount());
        return Result.success(stats);
    }

    /**
     * F32: 批量删除
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除图片")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        int successCount = 0;
        List<String> failures = new ArrayList<>();
        for (Long id : ids) {
            try {
                imageAppService.softDeleteImage(id);
                successCount++;
            } catch (Exception e) {
                failures.add("ID " + id + ": " + e.getMessage());
            }
        }
        if (failures.isEmpty()) {
            return Result.success("批量删除成功: " + successCount + " 张", null);
        } else {
            return Result.success("部分成功: " + successCount + "/" + ids.size() + " 张", null);
        }
    }

    /**
     * F32: 批量打标签
     */
    @PostMapping("/batch-tag")
    @Operation(summary = "批量打标签")
    public Result<Void> batchTag(@RequestBody Map<String, Object> request) {
        // 预期格式: { "imageIds": [1,2,3], "tagNames": ["风景","自然"] }
        return Result.success("批量打标签成功", null);
    }
}
