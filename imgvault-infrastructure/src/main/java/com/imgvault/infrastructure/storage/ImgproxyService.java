package com.imgvault.infrastructure.storage;

import com.imgvault.infrastructure.config.ImgproxyConfig;
import com.imgvault.infrastructure.config.MinioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * imgproxy 图片处理服务
 * <p>
 * 负责生成 imgproxy 签名 URL，支持:
 * - 缩略图生成 (resize)
 * - 格式转换 (WebP/AVIF)
 * - 水印添加 (watermark)
 * - 图片压缩 (quality)
 * - 智能裁剪 (gravity:sm)
 * <p>
 * 签名算法参考: https://github.com/imgproxy/imgproxy/blob/master/examples/signature.java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImgproxyService {

    private final ImgproxyConfig imgproxyConfig;
    private final MinioConfig minioConfig;

    private byte[] keyBytes;
    private byte[] saltBytes;

    @PostConstruct
    public void init() {
        if (imgproxyConfig.getKey() != null && imgproxyConfig.getSalt() != null) {
            this.keyBytes = hexStringToByteArray(imgproxyConfig.getKey());
            this.saltBytes = hexStringToByteArray(imgproxyConfig.getSalt());
            log.info("imgproxy 签名密钥已初始化");
        } else {
            log.warn("imgproxy 签名密钥未配置，URL 签名功能不可用");
        }
    }

    // ==================== 缩略图 URL (F12) ====================

    /**
     * 生成缩略图 URL（三种规格）
     *
     * @param storagePath MinIO 存储路径
     * @return 包含 small/medium/large 三种规格的 ThumbnailUrls
     */
    public ThumbnailUrls getThumbnailUrls(String storagePath) {
        ThumbnailUrls urls = new ThumbnailUrls();
        urls.setSmall(getResizeUrl(storagePath, 150, 150, "fit"));
        urls.setMedium(getResizeUrl(storagePath, 800, 600, "fit"));
        urls.setLarge(getResizeUrl(storagePath, 1920, 1080, "fit"));
        return urls;
    }

    /**
     * 生成指定尺寸的缩放 URL
     *
     * @param storagePath MinIO 存储路径
     * @param width       目标宽度
     * @param height      目标高度
     * @param resizeType  缩放类型: fit/fill/auto
     * @return 签名后的 imgproxy URL
     */
    public String getResizeUrl(String storagePath, int width, int height, String resizeType) {
        String processingPath = String.format("/rs:%s:%d:%d:0/plain/%s",
                resizeType, width, height, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    // ==================== 格式转换 URL (F14) ====================

    /**
     * 生成 WebP 格式 URL
     */
    public String getWebpUrl(String storagePath, int quality) {
        String processingPath = String.format("/q:%d/plain/%s@webp",
                quality, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    /**
     * 生成 AVIF 格式 URL
     */
    public String getAvifUrl(String storagePath, int quality) {
        String processingPath = String.format("/q:%d/plain/%s@avif",
                quality, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    /**
     * 生成指定格式的转换 URL
     *
     * @param storagePath MinIO 存储路径
     * @param format      目标格式: jpeg/png/webp/avif
     * @param quality     质量 1-100
     * @return 签名后的 imgproxy URL
     */
    public String getFormatConvertUrl(String storagePath, String format, int quality) {
        String processingPath = String.format("/q:%d/plain/%s@%s",
                quality, buildSourceUrl(storagePath), format);
        return buildSignedUrl(processingPath);
    }

    // ==================== 水印 URL (F15) ====================

    /**
     * 生成带水印的 URL
     *
     * @param storagePath MinIO 存储路径
     * @param opacity     水印透明度 0.0-1.0
     * @param position    水印位置: ce(中心)/no(北)/so(南)/we(西)/ea(东)/nowe/noea/sowe/soea
     * @return 签名后的 imgproxy URL
     */
    public String getWatermarkUrl(String storagePath, double opacity, String position) {
        String processingPath = String.format("/wm:%s:%s/plain/%s",
                formatDouble(opacity), position, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    // ==================== 图片压缩 URL (F16) ====================

    /**
     * 生成压缩后的图片 URL
     *
     * @param storagePath MinIO 存储路径
     * @param quality     质量 1-100
     * @return 签名后的 imgproxy URL
     */
    public String getCompressedUrl(String storagePath, int quality) {
        String processingPath = String.format("/q:%d/plain/%s",
                quality, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    // ==================== 智能裁剪 URL (F17) ====================

    /**
     * 生成智能裁剪 URL（基于内容感知）
     *
     * @param storagePath MinIO 存储路径
     * @param width       目标宽度
     * @param height      目标高度
     * @return 签名后的 imgproxy URL
     */
    public String getSmartCropUrl(String storagePath, int width, int height) {
        String processingPath = String.format("/rs:fill:%d:%d:0/g:sm/plain/%s",
                width, height, buildSourceUrl(storagePath));
        return buildSignedUrl(processingPath);
    }

    // ==================== 组合处理 URL ====================

    /**
     * 生成自定义处理参数的 URL
     *
     * @param storagePath      MinIO 存储路径
     * @param width            目标宽度 (0 表示不限制)
     * @param height           目标高度 (0 表示不限制)
     * @param format           输出格式 (null 表示保持原格式)
     * @param quality          质量 1-100 (0 表示默认)
     * @param smartCrop        是否智能裁剪
     * @return 签名后的 imgproxy URL
     */
    public String getProcessedUrl(String storagePath, int width, int height,
                                  String format, int quality, boolean smartCrop) {
        StringBuilder processingOpts = new StringBuilder();

        // 缩放
        if (width > 0 || height > 0) {
            String type = smartCrop ? "fill" : "fit";
            processingOpts.append(String.format("/rs:%s:%d:%d:0", type,
                    Math.max(width, 0), Math.max(height, 0)));
        }

        // 智能裁剪
        if (smartCrop) {
            processingOpts.append("/g:sm");
        }

        // 质量
        if (quality > 0 && quality <= 100) {
            processingOpts.append(String.format("/q:%d", quality));
        }

        // 源地址和格式
        String sourceUrl = buildSourceUrl(storagePath);
        if (format != null && !format.isEmpty()) {
            processingOpts.append(String.format("/plain/%s@%s", sourceUrl, format));
        } else {
            processingOpts.append(String.format("/plain/%s", sourceUrl));
        }

        return buildSignedUrl(processingOpts.toString());
    }

    // ==================== 内部方法 ====================

    /**
     * 构建 S3 源图片 URL
     * 格式: s3://bucket/path
     */
    private String buildSourceUrl(String storagePath) {
        return String.format("s3://%s/%s", minioConfig.getBucketName(), storagePath);
    }

    /**
     * 构建签名后的完整 URL
     * 如果配置了 externalBaseUrl，使用外部域名代理地址
     * 签名算法: HMAC-SHA256(key, salt + path) -> URL-safe Base64
     */
    private String buildSignedUrl(String path) {
        try {
            String signedPath = signPath(keyBytes, saltBytes, path);
            String baseUrl = imgproxyConfig.getExternalBaseUrl();
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = imgproxyConfig.getBaseUrl();
            }
            return baseUrl + signedPath;
        } catch (Exception e) {
            log.error("imgproxy URL 签名失败: path={}", path, e);
            throw new RuntimeException("imgproxy URL 签名失败: " + e.getMessage(), e);
        }
    }

    /**
     * imgproxy 官方签名算法
     * 参考: https://github.com/imgproxy/imgproxy/blob/master/examples/signature.java
     */
    private static String signPath(byte[] key, byte[] salt, String path) throws Exception {
        final String HMAC_SHA256 = "HmacSHA256";

        Mac sha256HMAC = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(key, HMAC_SHA256);
        sha256HMAC.init(secretKey);
        sha256HMAC.update(salt);

        String hash = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256HMAC.doFinal(path.getBytes()));

        return "/" + hash + path;
    }

    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] res = new byte[hex.length() / 2];
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) ((Character.digit(hex.charAt(i * 2), 16) << 4)
                    | (Character.digit(hex.charAt(i * 2 + 1), 16)));
        }
        return res;
    }

    private static String formatDouble(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.2f", value);
    }

    // ==================== DTO ====================

    /**
     * 缩略图 URL 集合
     */
    @lombok.Data
    public static class ThumbnailUrls {
        private String small;   // 150x150
        private String medium;  // 800x600
        private String large;   // 1920x1080
    }
}
