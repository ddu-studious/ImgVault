package com.imgvault.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片格式枚举
 */
@Getter
@AllArgsConstructor
public enum ImageFormat {

    JPEG("jpeg", "image/jpeg", new String[]{".jpg", ".jpeg"}),
    PNG("png", "image/png", new String[]{".png"}),
    GIF("gif", "image/gif", new String[]{".gif"}),
    WEBP("webp", "image/webp", new String[]{".webp"}),
    BMP("bmp", "image/bmp", new String[]{".bmp"});

    private final String format;
    private final String mimeType;
    private final String[] extensions;

    /**
     * 所有允许的 MIME 类型
     */
    private static final Set<String> ALLOWED_MIME_TYPES;

    static {
        Set<String> types = new HashSet<>();
        for (ImageFormat format : values()) {
            types.add(format.mimeType);
        }
        ALLOWED_MIME_TYPES = Collections.unmodifiableSet(types);
    }

    public static Set<String> getAllowedMimeTypes() {
        return ALLOWED_MIME_TYPES;
    }

    public static boolean isAllowed(String mimeType) {
        return ALLOWED_MIME_TYPES.contains(mimeType);
    }

    public static ImageFormat fromMimeType(String mimeType) {
        for (ImageFormat format : values()) {
            if (format.mimeType.equals(mimeType)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
    }

    public static ImageFormat fromExtension(String extension) {
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        for (ImageFormat format : values()) {
            for (String e : format.extensions) {
                if (e.equals(ext)) {
                    return format;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported extension: " + extension);
    }
}
