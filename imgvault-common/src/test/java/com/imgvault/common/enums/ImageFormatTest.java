package com.imgvault.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageFormat 枚举单元测试
 */
@DisplayName("ImageFormat 图片格式枚举")
class ImageFormatTest {

    @Nested
    @DisplayName("MIME 类型匹配")
    class MimeTypeTests {

        @Test
        @DisplayName("所有 5 种格式都在允许列表中")
        void allFormatsAreAllowed() {
            Set<String> allowed = ImageFormat.getAllowedMimeTypes();
            assertEquals(5, allowed.size());
            assertTrue(allowed.contains("image/jpeg"));
            assertTrue(allowed.contains("image/png"));
            assertTrue(allowed.contains("image/gif"));
            assertTrue(allowed.contains("image/webp"));
            assertTrue(allowed.contains("image/bmp"));
        }

        @Test
        @DisplayName("isAllowed 正确判断允许的 MIME 类型")
        void isAllowedForValidTypes() {
            assertTrue(ImageFormat.isAllowed("image/jpeg"));
            assertTrue(ImageFormat.isAllowed("image/png"));
            assertTrue(ImageFormat.isAllowed("image/gif"));
            assertTrue(ImageFormat.isAllowed("image/webp"));
            assertTrue(ImageFormat.isAllowed("image/bmp"));
        }

        @Test
        @DisplayName("isAllowed 拒绝非图片 MIME 类型")
        void isNotAllowedForInvalidTypes() {
            assertFalse(ImageFormat.isAllowed("text/plain"));
            assertFalse(ImageFormat.isAllowed("application/pdf"));
            assertFalse(ImageFormat.isAllowed("image/svg+xml"));
            assertFalse(ImageFormat.isAllowed("image/tiff"));
            assertFalse(ImageFormat.isAllowed(""));
            assertFalse(ImageFormat.isAllowed(null));
        }
    }

    @Nested
    @DisplayName("fromMimeType 转换")
    class FromMimeTypeTests {

        @Test
        @DisplayName("从 MIME 类型获取枚举值")
        void fromMimeType() {
            assertEquals(ImageFormat.JPEG, ImageFormat.fromMimeType("image/jpeg"));
            assertEquals(ImageFormat.PNG, ImageFormat.fromMimeType("image/png"));
            assertEquals(ImageFormat.GIF, ImageFormat.fromMimeType("image/gif"));
            assertEquals(ImageFormat.WEBP, ImageFormat.fromMimeType("image/webp"));
            assertEquals(ImageFormat.BMP, ImageFormat.fromMimeType("image/bmp"));
        }

        @Test
        @DisplayName("不支持的 MIME 类型抛出异常")
        void fromMimeTypeThrowsForUnsupported() {
            assertThrows(IllegalArgumentException.class,
                    () -> ImageFormat.fromMimeType("image/tiff"));
        }
    }

    @Nested
    @DisplayName("fromExtension 转换")
    class FromExtensionTests {

        @Test
        @DisplayName("从扩展名获取枚举值")
        void fromExtension() {
            assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension(".jpg"));
            assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension(".jpeg"));
            assertEquals(ImageFormat.PNG, ImageFormat.fromExtension(".png"));
            assertEquals(ImageFormat.GIF, ImageFormat.fromExtension(".gif"));
            assertEquals(ImageFormat.WEBP, ImageFormat.fromExtension(".webp"));
            assertEquals(ImageFormat.BMP, ImageFormat.fromExtension(".bmp"));
        }

        @Test
        @DisplayName("不带点号的扩展名也能识别")
        void fromExtensionWithoutDot() {
            assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("jpg"));
            assertEquals(ImageFormat.PNG, ImageFormat.fromExtension("png"));
        }

        @Test
        @DisplayName("不支持的扩展名抛出异常")
        void fromExtensionThrowsForUnsupported() {
            assertThrows(IllegalArgumentException.class,
                    () -> ImageFormat.fromExtension(".svg"));
            assertThrows(IllegalArgumentException.class,
                    () -> ImageFormat.fromExtension(".tiff"));
        }
    }

    @Nested
    @DisplayName("枚举属性")
    class PropertyTests {

        @Test
        @DisplayName("JPEG 属性正确")
        void jpegProperties() {
            ImageFormat jpeg = ImageFormat.JPEG;
            assertEquals("jpeg", jpeg.getFormat());
            assertEquals("image/jpeg", jpeg.getMimeType());
            assertArrayEquals(new String[]{".jpg", ".jpeg"}, jpeg.getExtensions());
        }

        @Test
        @DisplayName("PNG 属性正确")
        void pngProperties() {
            ImageFormat png = ImageFormat.PNG;
            assertEquals("png", png.getFormat());
            assertEquals("image/png", png.getMimeType());
            assertArrayEquals(new String[]{".png"}, png.getExtensions());
        }

        @Test
        @DisplayName("WebP 属性正确")
        void webpProperties() {
            ImageFormat webp = ImageFormat.WEBP;
            assertEquals("webp", webp.getFormat());
            assertEquals("image/webp", webp.getMimeType());
            assertArrayEquals(new String[]{".webp"}, webp.getExtensions());
        }
    }
}
