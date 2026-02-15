package com.imgvault.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MagicBytesValidator 单元测试
 * 验证文件类型检测和校验功能
 */
@DisplayName("MagicBytesValidator 文件类型校验")
class MagicBytesValidatorTest {

    // ==================== JPEG 测试 ====================

    @Nested
    @DisplayName("JPEG 格式检测")
    class JpegTests {

        private final byte[] JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};

        @Test
        @DisplayName("正确识别 JPEG 文件头")
        void shouldDetectJpeg() {
            assertEquals("image/jpeg", MagicBytesValidator.detectMimeType(JPEG_HEADER));
        }

        @Test
        @DisplayName("验证 JPEG MIME 类型匹配")
        void shouldValidateJpegMimeType() {
            assertTrue(MagicBytesValidator.validate(JPEG_HEADER, "image/jpeg"));
        }

        @Test
        @DisplayName("JPEG 文件头不匹配 PNG MIME 类型")
        void shouldNotMatchJpegAsPng() {
            assertFalse(MagicBytesValidator.validate(JPEG_HEADER, "image/png"));
        }

        @Test
        @DisplayName("JPEG 是合法图片格式")
        void shouldBeValidImage() {
            assertTrue(MagicBytesValidator.isValidImage(JPEG_HEADER));
        }
    }

    // ==================== PNG 测试 ====================

    @Nested
    @DisplayName("PNG 格式检测")
    class PngTests {

        private final byte[] PNG_HEADER = {(byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

        @Test
        @DisplayName("正确识别 PNG 文件头")
        void shouldDetectPng() {
            assertEquals("image/png", MagicBytesValidator.detectMimeType(PNG_HEADER));
        }

        @Test
        @DisplayName("验证 PNG MIME 类型匹配")
        void shouldValidatePngMimeType() {
            assertTrue(MagicBytesValidator.validate(PNG_HEADER, "image/png"));
        }

        @Test
        @DisplayName("PNG 是合法图片格式")
        void shouldBeValidImage() {
            assertTrue(MagicBytesValidator.isValidImage(PNG_HEADER));
        }
    }

    // ==================== GIF 测试 ====================

    @Nested
    @DisplayName("GIF 格式检测")
    class GifTests {

        // GIF89a
        private final byte[] GIF_HEADER = {0x47, 0x49, 0x46, 0x38,
                0x39, 0x61, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        @Test
        @DisplayName("正确识别 GIF 文件头")
        void shouldDetectGif() {
            assertEquals("image/gif", MagicBytesValidator.detectMimeType(GIF_HEADER));
        }

        @Test
        @DisplayName("验证 GIF MIME 类型匹配")
        void shouldValidateGifMimeType() {
            assertTrue(MagicBytesValidator.validate(GIF_HEADER, "image/gif"));
        }
    }

    // ==================== WebP 测试 ====================

    @Nested
    @DisplayName("WebP 格式检测")
    class WebpTests {

        // RIFF....WEBP
        private final byte[] WEBP_HEADER = {0x52, 0x49, 0x46, 0x46,
                0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};

        @Test
        @DisplayName("正确识别 WebP 文件头")
        void shouldDetectWebp() {
            assertEquals("image/webp", MagicBytesValidator.detectMimeType(WEBP_HEADER));
        }

        @Test
        @DisplayName("验证 WebP MIME 类型匹配")
        void shouldValidateWebpMimeType() {
            assertTrue(MagicBytesValidator.validate(WEBP_HEADER, "image/webp"));
        }
    }

    // ==================== BMP 测试 ====================

    @Nested
    @DisplayName("BMP 格式检测")
    class BmpTests {

        private final byte[] BMP_HEADER = {0x42, 0x4D, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        @Test
        @DisplayName("正确识别 BMP 文件头")
        void shouldDetectBmp() {
            assertEquals("image/bmp", MagicBytesValidator.detectMimeType(BMP_HEADER));
        }

        @Test
        @DisplayName("验证 BMP MIME 类型匹配")
        void shouldValidateBmpMimeType() {
            assertTrue(MagicBytesValidator.validate(BMP_HEADER, "image/bmp"));
        }
    }

    // ==================== 边界场景测试 ====================

    @Nested
    @DisplayName("边界场景和安全校验")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 输入返回 null")
        void shouldReturnNullForNullInput() {
            assertNull(MagicBytesValidator.detectMimeType(null));
        }

        @Test
        @DisplayName("空字节数组返回 null")
        void shouldReturnNullForEmptyArray() {
            assertNull(MagicBytesValidator.detectMimeType(new byte[0]));
        }

        @Test
        @DisplayName("字节数不足返回 null")
        void shouldReturnNullForShortArray() {
            assertNull(MagicBytesValidator.detectMimeType(new byte[]{0x01, 0x02}));
        }

        @Test
        @DisplayName("未知文件类型返回 null")
        void shouldReturnNullForUnknownType() {
            byte[] randomBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B};
            assertNull(MagicBytesValidator.detectMimeType(randomBytes));
        }

        @Test
        @DisplayName("null 输入 validate 返回 false")
        void shouldReturnFalseForNullValidation() {
            assertFalse(MagicBytesValidator.validate(null, "image/jpeg"));
        }

        @Test
        @DisplayName("伪造扩展名检测 - 文本文件声称是 JPEG")
        void shouldDetectFakeExtension() {
            // 文本文件头: "Hello" -> 不是任何图片格式
            byte[] textFile = {0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F,
                    0x72, 0x6C, 0x64, 0x21};
            assertFalse(MagicBytesValidator.validate(textFile, "image/jpeg"));
        }

        @Test
        @DisplayName("未知格式不是合法图片")
        void shouldNotBeValidImageForUnknownFormat() {
            byte[] randomBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
            assertFalse(MagicBytesValidator.isValidImage(randomBytes));
        }

        @Test
        @DisplayName("RIFF 但非 WebP 文件不识别为图片")
        void shouldNotDetectNonWebpRiffAsImage() {
            // RIFF 头但不是 WEBP（可能是 AVI 或 WAV）
            byte[] riffNonWebp = {0x52, 0x49, 0x46, 0x46,
                    0x00, 0x00, 0x00, 0x00, 0x41, 0x56, 0x49, 0x20};  // RIFF....AVI
            assertNull(MagicBytesValidator.detectMimeType(riffNonWebp));
        }
    }
}
