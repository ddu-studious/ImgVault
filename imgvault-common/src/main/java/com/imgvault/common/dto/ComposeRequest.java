package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "图片合成请求")
public class ComposeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull(message = "画布配置不能为空")
    @Schema(description = "画布配置")
    private CanvasConfig canvas;

    @Valid
    @NotEmpty(message = "至少需要一个图层")
    @Size(max = 30, message = "图层数量不能超过30")
    @Schema(description = "图层列表（按顺序从下到上绘制）")
    private List<LayerConfig> layers;

    @Schema(description = "输出配置")
    private OutputConfig output;

    @Data
    @Schema(description = "画布配置")
    public static class CanvasConfig implements Serializable {
        @Min(value = 1, message = "宽度最小为1")
        @Max(value = 4096, message = "宽度最大为4096")
        @Schema(description = "画布宽度", example = "1920")
        private int width = 1920;

        @Min(value = 1, message = "高度最小为1")
        @Max(value = 4096, message = "高度最大为4096")
        @Schema(description = "画布高度", example = "1080")
        private int height = 1080;

        @Schema(description = "背景色 (十六进制)", example = "#FFFFFF")
        private String backgroundColor = "#FFFFFF";
    }

    @Data
    @Schema(description = "图层配置")
    public static class LayerConfig implements Serializable {
        @NotNull(message = "图层类型不能为空")
        @Schema(description = "图层类型: image/text/shape", example = "image")
        private String type;

        // image 类型
        @Schema(description = "图片ID")
        private Long imageId;

        @Schema(description = "外部图片URL")
        private String imageUrl;

        // 通用定位
        @Schema(description = "X坐标", example = "0")
        private int x = 0;

        @Schema(description = "Y坐标", example = "0")
        private int y = 0;

        @Schema(description = "宽度 (0=自动)", example = "800")
        private int width = 0;

        @Schema(description = "高度 (0=自动)", example = "600")
        private int height = 0;

        @Schema(description = "透明度 0.0-1.0", example = "1.0")
        private float opacity = 1.0f;

        @Schema(description = "旋转角度 (度)", example = "0")
        private double rotation = 0;

        @Schema(description = "图片适配模式: cover/contain/fill/none", example = "cover")
        private String fit = "cover";

        @Schema(description = "圆角半径", example = "0")
        private int borderRadius = 0;

        // text 类型
        @Schema(description = "文字内容")
        private String content;

        @Schema(description = "字体大小", example = "48")
        private int fontSize = 48;

        @Schema(description = "字体", example = "SansSerif")
        private String fontFamily = "SansSerif";

        @Schema(description = "字体粗细: normal/bold", example = "normal")
        private String fontWeight = "normal";

        @Schema(description = "文字颜色", example = "#333333")
        private String color = "#333333";

        @Schema(description = "最大宽度 (0=不限制，超出自动换行)", example = "0")
        private int maxWidth = 0;

        @Schema(description = "行高倍数", example = "1.5")
        private float lineHeight = 1.5f;

        @Schema(description = "文字对齐: left/center/right", example = "left")
        private String textAlign = "left";

        // shape 类型
        @Schema(description = "形状: rect/circle/line")
        private String shape;

        @Schema(description = "边框颜色")
        private String borderColor;

        @Schema(description = "边框宽度")
        private int borderWidth = 0;
    }

    @Data
    @Schema(description = "输出配置")
    public static class OutputConfig implements Serializable {
        @Schema(description = "输出格式: png/jpeg/webp", example = "png")
        private String format = "png";

        @Min(value = 1, message = "质量最小为1")
        @Max(value = 100, message = "质量最大为100")
        @Schema(description = "质量 1-100", example = "90")
        private int quality = 90;
    }
}
