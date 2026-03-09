package com.imgvault.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * OpenCV 风格的水印去除服务
 * 使用纯 Java BufferedImage 实现简易 inpainting 算法，适用于简单水印
 */
@Slf4j
@Service
public class OpenCvWatermarkService {

    private static final int MASK_WHITE_THRESHOLD = 128;
    private static final int DEFAULT_RADIUS = 5;
    private static final int DEFAULT_ITERATIONS = 10;

    @Value("${watermark.opencv.radius:5}")
    private int radius = DEFAULT_RADIUS;

    @Value("${watermark.opencv.iterations:10}")
    private int iterations = DEFAULT_ITERATIONS;

    /**
     * 去除水印（简易 Telea 风格 inpainting）
     *
     * @param imageData 源图片字节
     * @param maskData  遮罩 PNG 字节，白色区域为水印
     * @return 处理后的图片字节（JPEG/PNG）
     */
    public byte[] removeWatermark(byte[] imageData, byte[] maskData) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IllegalArgumentException("无法解析源图片");
        }
        BufferedImage mask = ImageIO.read(new ByteArrayInputStream(maskData));
        if (mask == null) {
            throw new IllegalArgumentException("无法解析遮罩图片");
        }
        if (image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight()) {
            throw new IllegalArgumentException("图片与遮罩尺寸不一致");
        }

        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        int w = image.getWidth();
        int h = image.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                result.setRGB(x, y, image.getRGB(x, y));
            }
        }

        for (int iter = 0; iter < iterations; iter++) {
            BufferedImage prev = copyImage(result);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (isMaskPixel(mask, x, y)) {
                        int avg = computeAverageOfNeighbors(prev, mask, x, y, w, h);
                        if (avg >= 0) {
                            result.setRGB(x, y, avg);
                        }
                    }
                }
            }
        }

        return encodeAsJpegOrPng(result, imageData);
    }

    private boolean isMaskPixel(BufferedImage mask, int x, int y) {
        int rgb = mask.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int gray = (r + g + b) / 3;
        return gray >= MASK_WHITE_THRESHOLD;
    }

    private int computeAverageOfNeighbors(BufferedImage image, BufferedImage mask,
                                           int cx, int cy, int w, int h) {
        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;
        int r = radius;

        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (isMaskPixel(mask, nx, ny)) continue;

                int rgb = image.getRGB(nx, ny);
                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                count++;
            }
        }
        if (count == 0) return -1;
        int avgR = (int) (sumR / count);
        int avgG = (int) (sumG / count);
        int avgB = (int) (sumB / count);
        return (0xFF << 24) | (avgR << 16) | (avgG << 8) | avgB;
    }

    private BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setRGB(x, y, src.getRGB(x, y));
            }
        }
        return copy;
    }

    private byte[] encodeAsJpegOrPng(BufferedImage image, byte[] originalData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        if (hasAlpha) {
            ImageIO.write(image, "png", baos);
        } else {
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            ImageIO.write(rgbImage, "jpeg", baos);
        }
        return baos.toByteArray();
    }
}
