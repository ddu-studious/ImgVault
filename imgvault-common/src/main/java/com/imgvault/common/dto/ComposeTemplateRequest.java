package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "模板合成请求")
public class ComposeTemplateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "模板ID: grid-2x2, grid-3x3, grid-1-2, grid-1-3, horizontal, vertical, poster, comparison",
            example = "grid-2x2")
    private String template;

    @NotEmpty(message = "至少需要一张图片")
    @Schema(description = "图片ID列表")
    private List<Long> images;

    @Schema(description = "文字参数 (title, subtitle, description 等)")
    private Map<String, String> text;

    @Schema(description = "间距 (像素)", example = "10")
    private int gap = 10;

    @Schema(description = "输出配置")
    private ComposeRequest.OutputConfig output;
}
