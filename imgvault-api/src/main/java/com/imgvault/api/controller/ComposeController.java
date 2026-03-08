package com.imgvault.api.controller;

import com.imgvault.app.service.ComposeAppService;
import com.imgvault.common.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compose")
@RequiredArgsConstructor
@Tag(name = "图片合成", description = "多图+文字合成为一张图片")
public class ComposeController {

    private final ComposeAppService composeAppService;

    @PostMapping
    @Operation(summary = "自由画布合成", description = "指定画布尺寸和图层列表，合成一张图片")
    public Result<ComposeResultDTO> compose(
            @Valid @RequestBody ComposeRequest request,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        ComposeResultDTO result = composeAppService.compose(request, visitorId);
        return Result.success(result);
    }

    @PostMapping("/template")
    @Operation(summary = "模板合成", description = "选择模板，指定图片和文字，自动生成合成图")
    public Result<ComposeResultDTO> composeByTemplate(
            @Valid @RequestBody ComposeTemplateRequest request,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {
        ComposeResultDTO result = composeAppService.composeByTemplate(request, visitorId);
        return Result.success(result);
    }

    @GetMapping("/templates")
    @Operation(summary = "获取模板列表", description = "返回所有可用合成模板")
    public Result<List<Map<String, Object>>> listTemplates() {
        return Result.success(composeAppService.listTemplates());
    }
}
