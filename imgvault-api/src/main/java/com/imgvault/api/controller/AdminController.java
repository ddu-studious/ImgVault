package com.imgvault.api.controller;

import com.imgvault.api.config.AdminTokenUtil;
import com.imgvault.app.service.AsyncTaskService;
import com.imgvault.app.service.ImageAppService;
import com.imgvault.app.service.OperationLogService;
import com.imgvault.app.service.TagAppService;
import com.imgvault.common.dto.ImageDetailDTO;
import com.imgvault.common.dto.ImageQueryRequest;
import com.imgvault.common.dto.PageResult;
import com.imgvault.common.dto.Result;
import com.imgvault.domain.entity.OperationLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name="Admin",description="Admin management APIs")
public class AdminController {
    private final ImageAppService imageAppService;
    private final AsyncTaskService asyncTaskService;
    private final OperationLogService operationLogService;
    private final TagAppService tagAppService;
    private final AdminTokenUtil adminTokenUtil;

    @Value("${admin.password:imgvault-admin}")
    private String adminPassword;

    @Value("${admin.jwt.expire-hours:24}")
    private int expireHours;

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "验证密码后返回 Token")
    public Result<?> login(@RequestBody Map<String, String> req) {
        String password = req.get("password");
        if (password != null && password.equals(adminPassword)) {
            String token = adminTokenUtil.generateToken();
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("expiresIn", expireHours * 3600);
            return Result.success(data);
        }
        return Result.fail(401, "密码错误");
    }

    @GetMapping("/images")
    public Result<PageResult<ImageDetailDTO>> listImages(@javax.validation.Valid ImageQueryRequest request) {
        return Result.success(imageAppService.listImages(request));
    }

    @GetMapping("/logs")
    public Result<List<OperationLogEntity>> getRecentLogs(@RequestParam(defaultValue="50") int limit) {
        return Result.success(operationLogService.getRecentLogs(limit));
    }

    @GetMapping("/logs/{targetType}/{targetId}")
    public Result<List<OperationLogEntity>> getTargetLogs(@PathVariable String targetType, @PathVariable Long targetId, @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        return Result.success(operationLogService.getTargetLogs(targetType, targetId, page, size));
    }

    @GetMapping("/trash")
    public Result<PageResult<ImageDetailDTO>> listTrash(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        ImageQueryRequest r = new ImageQueryRequest();
        r.setPage(page); r.setSize(size); r.setStatus(0);
        return Result.success(imageAppService.listImages(r));
    }

    @PostMapping("/trash/{id}/restore")
    public Result<Void> restoreImage(@PathVariable Long id) {
        imageAppService.restoreImage(id);
        return Result.success("Restored", null);
    }

    @GetMapping("/stats")
    public Result<Map<String,Object>> getStats() {
        Map<String,Object> s = imageAppService.getAdminStats();
        s.put("asyncTasks", asyncTaskService.getTaskStats());
        s.put("totalLogs", operationLogService.getTotalCount());
        return Result.success(s);
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        int ok=0;
        for (Long id : ids) { try { imageAppService.softDeleteImage(id); ok++; } catch (Exception e) {} }
        return Result.success("Deleted: "+ok+"/"+ids.size(), null);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/batch-tag")
    public Result<Void> batchTag(@RequestBody Map<String,Object> req) {
        List<Number> ids=(List<Number>)req.get("imageIds");
        List<String> tags=(List<String>)req.get("tagNames");
        if(ids==null||tags==null||ids.isEmpty()||tags.isEmpty()) return Result.fail(400,"imageIds and tagNames required");
        int ok=0;
        for(Number i:ids){try{tagAppService.addTagsToImage(i.longValue(),tags);ok++;}catch(Exception e){}}
        return Result.success("Tagged: "+ok+"/"+ids.size(),null);
    }
}
