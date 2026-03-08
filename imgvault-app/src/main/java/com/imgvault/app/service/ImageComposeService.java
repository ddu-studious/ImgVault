package com.imgvault.app.service;

import com.imgvault.common.dto.ComposeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.List;

/**
 * 图片合成引擎
 * 基于 Java Graphics2D 实现多图层画布合成
 */
@Slf4j
@Service
public class ImageComposeService {

    /**
     * 执行合成
     */
    public ComposeOutput compose(ComposeRequest.CanvasConfig canvas,
                                 List<LayerInput> layerInputs,
                                 ComposeRequest.OutputConfig output) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        setupRenderingHints(g);
        drawBackground(g, w, h, canvas.getBackgroundColor());

        for (LayerInput input : layerInputs) {
            try {
                drawLayer(g, input);
            } catch (Exception e) {
                log.warn("绘制图层失败: type={}, reason={}", input.getConfig().getType(), e.getMessage());
            }
        }

        g.dispose();

        String fmt = output != null && output.getFormat() != null ? output.getFormat() : "png";
        int quality = output != null ? output.getQuality() : 90;

        byte[] data = encodeImage(result, fmt, quality);

        ComposeOutput out = new ComposeOutput();
        out.setData(data);
        out.setWidth(w);
        out.setHeight(h);
        out.setFormat(fmt);
        return out;
    }

    private void setupRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private void drawBackground(Graphics2D g, int w, int h, String bgColor) {
        g.setColor(parseColor(bgColor, Color.WHITE));
        g.fillRect(0, 0, w, h);
    }

    private void drawLayer(Graphics2D g, LayerInput input) throws IOException {
        ComposeRequest.LayerConfig cfg = input.getConfig();
        String type = cfg.getType();

        if ("image".equals(type)) {
            drawImageLayer(g, input);
        } else if ("text".equals(type)) {
            drawTextLayer(g, cfg);
        } else if ("shape".equals(type)) {
            drawShapeLayer(g, cfg);
        }
    }

    // ==================== 图片图层 ====================

    private void drawImageLayer(Graphics2D g, LayerInput input) throws IOException {
        if (input.getImageData() == null) return;

        BufferedImage src = ImageIO.read(new ByteArrayInputStream(input.getImageData()));
        if (src == null) return;

        ComposeRequest.LayerConfig cfg = input.getConfig();
        int targetW = cfg.getWidth() > 0 ? cfg.getWidth() : src.getWidth();
        int targetH = cfg.getHeight() > 0 ? cfg.getHeight() : src.getHeight();

        BufferedImage fitted = fitImage(src, targetW, targetH, cfg.getFit());

        if (cfg.getBorderRadius() > 0) {
            fitted = applyRoundedCorners(fitted, cfg.getBorderRadius());
        }

        Composite originalComposite = g.getComposite();

        if (cfg.getOpacity() < 1.0f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cfg.getOpacity()));
        }

        if (cfg.getRotation() != 0) {
            AffineTransform old = g.getTransform();
            double cx = cfg.getX() + targetW / 2.0;
            double cy = cfg.getY() + targetH / 2.0;
            g.rotate(Math.toRadians(cfg.getRotation()), cx, cy);
            g.drawImage(fitted, cfg.getX(), cfg.getY(), targetW, targetH, null);
            g.setTransform(old);
        } else {
            g.drawImage(fitted, cfg.getX(), cfg.getY(), targetW, targetH, null);
        }

        g.setComposite(originalComposite);
    }

    private BufferedImage fitImage(BufferedImage src, int targetW, int targetH, String fit) {
        if ("none".equals(fit)) return src;

        int srcW = src.getWidth();
        int srcH = src.getHeight();
        double srcRatio = (double) srcW / srcH;
        double targetRatio = (double) targetW / targetH;

        int drawW, drawH, cropX = 0, cropY = 0;

        if ("cover".equals(fit)) {
            if (srcRatio > targetRatio) {
                drawH = srcH;
                drawW = (int) (srcH * targetRatio);
                cropX = (srcW - drawW) / 2;
            } else {
                drawW = srcW;
                drawH = (int) (srcW / targetRatio);
                cropY = (srcH - drawH) / 2;
            }
            BufferedImage cropped = src.getSubimage(cropX, cropY,
                    Math.min(drawW, srcW - cropX), Math.min(drawH, srcH - cropY));
            return resize(cropped, targetW, targetH);
        } else if ("contain".equals(fit)) {
            double scale;
            if (srcRatio > targetRatio) {
                scale = (double) targetW / srcW;
            } else {
                scale = (double) targetH / srcH;
            }
            int newW = (int) (srcW * scale);
            int newH = (int) (srcH * scale);
            BufferedImage canvas = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = canvas.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            cg.drawImage(src, (targetW - newW) / 2, (targetH - newH) / 2, newW, newH, null);
            cg.dispose();
            return canvas;
        } else {
            return resize(src, targetW, targetH);
        }
    }

    private BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return result;
    }

    private BufferedImage applyRoundedCorners(BufferedImage src, int radius) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return result;
    }

    // ==================== 文字图层 ====================

    private void drawTextLayer(Graphics2D g, ComposeRequest.LayerConfig cfg) {
        if (cfg.getContent() == null || cfg.getContent().isEmpty()) return;

        int style = "bold".equalsIgnoreCase(cfg.getFontWeight()) ? Font.BOLD : Font.PLAIN;
        Font font = new Font(cfg.getFontFamily(), style, cfg.getFontSize());
        g.setFont(font);

        Color textColor = parseColor(cfg.getColor(), Color.BLACK);

        Composite originalComposite = g.getComposite();
        if (cfg.getOpacity() < 1.0f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cfg.getOpacity()));
        }

        g.setColor(textColor);

        if (cfg.getMaxWidth() > 0) {
            drawWrappedText(g, cfg.getContent(), font, cfg.getX(), cfg.getY(),
                    cfg.getMaxWidth(), cfg.getLineHeight(), cfg.getTextAlign());
        } else {
            FontMetrics fm = g.getFontMetrics();
            int textX = cfg.getX();
            if ("center".equals(cfg.getTextAlign())) {
                textX -= fm.stringWidth(cfg.getContent()) / 2;
            } else if ("right".equals(cfg.getTextAlign())) {
                textX -= fm.stringWidth(cfg.getContent());
            }
            g.drawString(cfg.getContent(), textX, cfg.getY() + fm.getAscent());
        }

        g.setComposite(originalComposite);
    }

    private void drawWrappedText(Graphics2D g, String text, Font font,
                                 int x, int y, int maxWidth, float lineHeight, String align) {
        AttributedString attrStr = new AttributedString(text);
        attrStr.addAttribute(TextAttribute.FONT, font);

        FontRenderContext frc = g.getFontRenderContext();
        LineBreakMeasurer measurer = new LineBreakMeasurer(attrStr.getIterator(), frc);

        float drawY = y;

        while (measurer.getPosition() < text.length()) {
            TextLayout layout = measurer.nextLayout(maxWidth);
            drawY += layout.getAscent();

            float drawX = x;
            if ("center".equals(align)) {
                drawX = x + (maxWidth - layout.getAdvance()) / 2;
            } else if ("right".equals(align)) {
                drawX = x + maxWidth - layout.getAdvance();
            }

            layout.draw(g, drawX, drawY);
            drawY += layout.getDescent() + layout.getLeading();
            drawY += (lineHeight - 1.0f) * font.getSize();
        }
    }

    // ==================== 形状图层 ====================

    private void drawShapeLayer(Graphics2D g, ComposeRequest.LayerConfig cfg) {
        if (cfg.getShape() == null) return;

        Composite originalComposite = g.getComposite();
        if (cfg.getOpacity() < 1.0f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cfg.getOpacity()));
        }

        Color fillColor = parseColor(cfg.getColor(), null);
        Color borderColor = parseColor(cfg.getBorderColor(), null);

        Shape shape;
        switch (cfg.getShape()) {
            case "circle":
                int diameter = Math.min(cfg.getWidth(), cfg.getHeight());
                shape = new Ellipse2D.Float(cfg.getX(), cfg.getY(), diameter, diameter);
                break;
            case "rect":
            default:
                if (cfg.getBorderRadius() > 0) {
                    shape = new RoundRectangle2D.Float(cfg.getX(), cfg.getY(),
                            cfg.getWidth(), cfg.getHeight(),
                            cfg.getBorderRadius(), cfg.getBorderRadius());
                } else {
                    shape = new Rectangle(cfg.getX(), cfg.getY(), cfg.getWidth(), cfg.getHeight());
                }
                break;
        }

        if (fillColor != null) {
            g.setColor(fillColor);
            g.fill(shape);
        }

        if (borderColor != null && cfg.getBorderWidth() > 0) {
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(cfg.getBorderWidth()));
            g.draw(shape);
        }

        g.setComposite(originalComposite);
    }

    // ==================== 工具方法 ====================

    private Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            if (hex.startsWith("rgba")) {
                String inner = hex.substring(5, hex.length() - 1);
                String[] parts = inner.split(",");
                return new Color(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        (int) (Float.parseFloat(parts[3].trim()) * 255));
            }
            hex = hex.replace("#", "");
            if (hex.length() == 6) {
                return new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16));
            } else if (hex.length() == 8) {
                return new Color(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16),
                        Integer.parseInt(hex.substring(6, 8), 16));
            }
        } catch (Exception e) {
            log.debug("解析颜色失败: {}", hex);
        }
        return defaultColor;
    }

    private byte[] encodeImage(BufferedImage image, String format, int quality) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if ("jpeg".equals(format) || "jpg".equals(format)) {
                BufferedImage rgbImage = new BufferedImage(
                        image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImage.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();

                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality / 100.0f);
                writer.setOutput(ImageIO.createImageOutputStream(baos));
                writer.write(null, new IIOImage(rgbImage, null, null), param);
                writer.dispose();
            } else {
                ImageIO.write(image, "png".equals(format) ? "png" : format, baos);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("图片编码失败: " + e.getMessage(), e);
        }
    }

    // ==================== 输入/输出 DTO ====================

    @lombok.Data
    public static class LayerInput {
        private ComposeRequest.LayerConfig config;
        private byte[] imageData;
    }

    @lombok.Data
    public static class ComposeOutput {
        private byte[] data;
        private int width;
        private int height;
        private String format;
    }
}
