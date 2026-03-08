package com.imgvault.app.service;

import com.imgvault.common.dto.ComposeRequest;
import com.imgvault.common.dto.ComposeTemplateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 合成模板引擎
 * 将模板 + 参数 转为画布配置 + 图层列表
 */
@Slf4j
@Component
public class ComposeTemplateEngine {

    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;

    public static class TemplateInfo {
        public final String id;
        public final String name;
        public final String description;
        public final int minImages;
        public final int maxImages;

        TemplateInfo(String id, String name, String description, int minImages, int maxImages) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.minImages = minImages;
            this.maxImages = maxImages;
        }
    }

    public List<TemplateInfo> listTemplates() {
        return Arrays.asList(
                new TemplateInfo("grid-2x2", "2×2 网格", "4张图等分排列", 4, 4),
                new TemplateInfo("grid-3x3", "3×3 网格", "9张图等分排列", 9, 9),
                new TemplateInfo("grid-1-2", "1+2 布局", "左侧大图 + 右侧2张小图", 3, 3),
                new TemplateInfo("grid-1-3", "1+3 布局", "顶部大图 + 底部3张小图", 4, 4),
                new TemplateInfo("horizontal", "横向拼接", "N张图水平排列", 2, 10),
                new TemplateInfo("vertical", "纵向拼接", "N张图垂直排列", 2, 10),
                new TemplateInfo("poster", "海报", "背景图 + 标题 + 副标题", 1, 1),
                new TemplateInfo("comparison", "对比图", "左右两图对比 + 标签", 2, 2)
        );
    }

    /**
     * 根据模板生成合成请求
     */
    public ComposeRequest buildFromTemplate(ComposeTemplateRequest req) {
        String templateId = req.getTemplate();
        List<Long> images = req.getImages();
        Map<String, String> text = req.getText() != null ? req.getText() : Collections.emptyMap();
        int gap = Math.max(0, req.getGap());

        ComposeRequest result = new ComposeRequest();
        ComposeRequest.CanvasConfig canvas = new ComposeRequest.CanvasConfig();
        List<ComposeRequest.LayerConfig> layers = new ArrayList<>();

        int outW = DEFAULT_WIDTH;
        int outH = DEFAULT_HEIGHT;

        if (req.getOutput() != null && req.getOutput().getFormat() != null) {
            result.setOutput(req.getOutput());
        }

        switch (templateId) {
            case "grid-2x2":
                outW = DEFAULT_WIDTH;
                outH = DEFAULT_WIDTH;
                buildGrid(layers, images, 2, 2, outW, outH, gap);
                break;

            case "grid-3x3":
                outW = DEFAULT_WIDTH;
                outH = DEFAULT_WIDTH;
                buildGrid(layers, images, 3, 3, outW, outH, gap);
                break;

            case "grid-1-2":
                buildGrid12(layers, images, outW, outH, gap);
                break;

            case "grid-1-3":
                buildGrid13(layers, images, outW, outH, gap);
                break;

            case "horizontal":
                outW = 400 * images.size() + gap * (images.size() - 1);
                outH = 400;
                buildHorizontal(layers, images, outW, outH, gap);
                break;

            case "vertical":
                outW = 800;
                outH = 600 * images.size() + gap * (images.size() - 1);
                buildVertical(layers, images, outW, outH, gap);
                break;

            case "poster":
                buildPoster(layers, images, text, outW, outH);
                break;

            case "comparison":
                buildComparison(layers, images, text, outW, outH, gap);
                break;

            default:
                throw new IllegalArgumentException("不支持的模板: " + templateId);
        }

        canvas.setWidth(outW);
        canvas.setHeight(outH);
        canvas.setBackgroundColor("#FFFFFF");
        result.setCanvas(canvas);
        result.setLayers(layers);

        return result;
    }

    private void buildGrid(List<ComposeRequest.LayerConfig> layers,
                           List<Long> images, int cols, int rows,
                           int canvasW, int canvasH, int gap) {
        int cellW = (canvasW - gap * (cols - 1)) / cols;
        int cellH = (canvasH - gap * (rows - 1)) / rows;

        for (int i = 0; i < Math.min(images.size(), cols * rows); i++) {
            int col = i % cols;
            int row = i / cols;
            ComposeRequest.LayerConfig layer = new ComposeRequest.LayerConfig();
            layer.setType("image");
            layer.setImageId(images.get(i));
            layer.setX(col * (cellW + gap));
            layer.setY(row * (cellH + gap));
            layer.setWidth(cellW);
            layer.setHeight(cellH);
            layer.setFit("cover");
            layers.add(layer);
        }
    }

    private void buildGrid12(List<ComposeRequest.LayerConfig> layers,
                             List<Long> images, int w, int h, int gap) {
        int leftW = (w - gap) * 2 / 3;
        int rightW = w - leftW - gap;
        int smallH = (h - gap) / 2;

        addImageLayer(layers, images, 0, 0, 0, leftW, h);
        addImageLayer(layers, images, 1, leftW + gap, 0, rightW, smallH);
        addImageLayer(layers, images, 2, leftW + gap, smallH + gap, rightW, smallH);
    }

    private void buildGrid13(List<ComposeRequest.LayerConfig> layers,
                             List<Long> images, int w, int h, int gap) {
        int topH = (h - gap) * 2 / 3;
        int bottomH = h - topH - gap;
        int cellW = (w - gap * 2) / 3;

        addImageLayer(layers, images, 0, 0, 0, w, topH);
        for (int i = 0; i < 3 && i + 1 < images.size(); i++) {
            addImageLayer(layers, images, i + 1, i * (cellW + gap), topH + gap, cellW, bottomH);
        }
    }

    private void buildHorizontal(List<ComposeRequest.LayerConfig> layers,
                                 List<Long> images, int w, int h, int gap) {
        int cellW = (w - gap * (images.size() - 1)) / images.size();
        for (int i = 0; i < images.size(); i++) {
            addImageLayer(layers, images, i, i * (cellW + gap), 0, cellW, h);
        }
    }

    private void buildVertical(List<ComposeRequest.LayerConfig> layers,
                               List<Long> images, int w, int h, int gap) {
        int cellH = (h - gap * (images.size() - 1)) / images.size();
        for (int i = 0; i < images.size(); i++) {
            addImageLayer(layers, images, i, 0, i * (cellH + gap), w, cellH);
        }
    }

    private void buildPoster(List<ComposeRequest.LayerConfig> layers,
                             List<Long> images, Map<String, String> text,
                             int w, int h) {
        addImageLayer(layers, images, 0, 0, 0, w, h);

        ComposeRequest.LayerConfig overlay = new ComposeRequest.LayerConfig();
        overlay.setType("shape");
        overlay.setShape("rect");
        overlay.setX(0);
        overlay.setY(h / 2);
        overlay.setWidth(w);
        overlay.setHeight(h / 2);
        overlay.setColor("rgba(0,0,0,0.4)");
        layers.add(overlay);

        if (text.containsKey("title")) {
            ComposeRequest.LayerConfig title = new ComposeRequest.LayerConfig();
            title.setType("text");
            title.setContent(text.get("title"));
            title.setX(80);
            title.setY(h - 200);
            title.setFontSize(72);
            title.setFontWeight("bold");
            title.setColor("#FFFFFF");
            title.setMaxWidth(w - 160);
            layers.add(title);
        }

        if (text.containsKey("subtitle")) {
            ComposeRequest.LayerConfig subtitle = new ComposeRequest.LayerConfig();
            subtitle.setType("text");
            subtitle.setContent(text.get("subtitle"));
            subtitle.setX(80);
            subtitle.setY(h - 100);
            subtitle.setFontSize(36);
            subtitle.setColor("#CCCCCC");
            subtitle.setMaxWidth(w - 160);
            layers.add(subtitle);
        }
    }

    private void buildComparison(List<ComposeRequest.LayerConfig> layers,
                                 List<Long> images, Map<String, String> text,
                                 int w, int h, int gap) {
        int halfW = (w - gap) / 2;

        addImageLayer(layers, images, 0, 0, 0, halfW, h);
        addImageLayer(layers, images, 1, halfW + gap, 0, halfW, h);

        ComposeRequest.LayerConfig divider = new ComposeRequest.LayerConfig();
        divider.setType("shape");
        divider.setShape("rect");
        divider.setX(halfW);
        divider.setY(0);
        divider.setWidth(gap);
        divider.setHeight(h);
        divider.setColor("#333333");
        layers.add(divider);

        String leftLabel = text.getOrDefault("left", "Before");
        String rightLabel = text.getOrDefault("right", "After");

        ComposeRequest.LayerConfig leftText = new ComposeRequest.LayerConfig();
        leftText.setType("text");
        leftText.setContent(leftLabel);
        leftText.setX(20);
        leftText.setY(h - 60);
        leftText.setFontSize(32);
        leftText.setFontWeight("bold");
        leftText.setColor("#FFFFFF");
        layers.add(leftText);

        ComposeRequest.LayerConfig rightText = new ComposeRequest.LayerConfig();
        rightText.setType("text");
        rightText.setContent(rightLabel);
        rightText.setX(halfW + gap + 20);
        rightText.setY(h - 60);
        rightText.setFontSize(32);
        rightText.setFontWeight("bold");
        rightText.setColor("#FFFFFF");
        layers.add(rightText);
    }

    private void addImageLayer(List<ComposeRequest.LayerConfig> layers,
                               List<Long> images, int idx,
                               int x, int y, int w, int h) {
        if (idx >= images.size()) return;
        ComposeRequest.LayerConfig layer = new ComposeRequest.LayerConfig();
        layer.setType("image");
        layer.setImageId(images.get(idx));
        layer.setX(x);
        layer.setY(y);
        layer.setWidth(w);
        layer.setHeight(h);
        layer.setFit("cover");
        layers.add(layer);
    }
}
