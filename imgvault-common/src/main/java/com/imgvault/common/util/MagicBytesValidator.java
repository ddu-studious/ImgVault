package com.imgvault.common.util;

import com.imgvault.common.enums.ImageFormat;

import java.util.Arrays;

/**
 * Magic Bytes 文件类型校验工具
 * 通过文件头部字节验证真实文件类型，防止伪造扩展名上传
 */
public final class MagicBytesValidator {

    private MagicBytesValidator() {
    }

    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    // GIF: 47 49 46 38 (GIF8)
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};

    // BMP: 42 4D (BM)
    private static final byte[] BMP_MAGIC = {0x42, 0x4D};

    // WebP: 52 49 46 46 xx xx xx xx 57 45 42 50 (RIFF....WEBP)
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    /**
     * 验证文件 Magic Bytes 是否匹配声称的 MIME 类型
     *
     * @param fileHeader 文件头部字节（至少 12 字节）
     * @param claimedMimeType 声称的 MIME 类型
     * @return true 如果 Magic Bytes 匹配
     */
    public static boolean validate(byte[] fileHeader, String claimedMimeType) {
        if (fileHeader == null || fileHeader.length < 4) {
            return false;
        }

        String detectedMimeType = detectMimeType(fileHeader);
        if (detectedMimeType == null) {
            return false;
        }

        return detectedMimeType.equals(claimedMimeType);
    }

    /**
     * 通过 Magic Bytes 检测真实 MIME 类型
     *
     * @param fileHeader 文件头部字节（至少 12 字节）
     * @return 检测到的 MIME 类型，未知类型返回 null
     */
    public static String detectMimeType(byte[] fileHeader) {
        if (fileHeader == null || fileHeader.length < 4) {
            return null;
        }

        // JPEG
        if (startsWith(fileHeader, JPEG_MAGIC)) {
            return "image/jpeg";
        }

        // PNG
        if (fileHeader.length >= 8 && startsWith(fileHeader, PNG_MAGIC)) {
            return "image/png";
        }

        // GIF
        if (startsWith(fileHeader, GIF_MAGIC)) {
            return "image/gif";
        }

        // WebP (RIFF....WEBP)
        if (fileHeader.length >= 12 && startsWith(fileHeader, RIFF_MAGIC)) {
            byte[] webpCheck = Arrays.copyOfRange(fileHeader, 8, 12);
            if (Arrays.equals(webpCheck, WEBP_MAGIC)) {
                return "image/webp";
            }
        }

        // BMP
        if (startsWith(fileHeader, BMP_MAGIC)) {
            return "image/bmp";
        }

        return null;
    }

    /**
     * 检测文件是否为合法的图片格式
     *
     * @param fileHeader 文件头部字节
     * @return true 如果是合法图片
     */
    public static boolean isValidImage(byte[] fileHeader) {
        String mimeType = detectMimeType(fileHeader);
        return mimeType != null && ImageFormat.isAllowed(mimeType);
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
