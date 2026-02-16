package com.imgvault.api.config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 轻量级 Admin Token 工具
 * 基于 HMAC-SHA256 签名，无需引入额外 JWT 依赖
 */
public class AdminTokenUtil {

    private final String secret;
    private final long expireMs;

    public AdminTokenUtil(String secret, int expireHours) {
        this.secret = secret;
        this.expireMs = expireHours * 3600L * 1000L;
    }

    /**
     * 生成 Token
     * 格式: base64url(payload).base64url(hmac-sha256(payload, secret))
     */
    public String generateToken() {
        long exp = System.currentTimeMillis() + expireMs;
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"exp\":" + exp + ",\"role\":\"admin\"}").getBytes(StandardCharsets.UTF_8));
        return payload + "." + hmacSign(payload);
    }

    /**
     * 验证 Token 有效性（签名 + 过期时间）
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return false;
            }

            String payload = parts[0];
            String signature = parts[1];

            // 验证签名
            if (!hmacSign(payload).equals(signature)) {
                return false;
            }

            // 验证过期时间
            String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            int expIdx = json.indexOf("\"exp\":");
            if (expIdx < 0) {
                return false;
            }
            String expStr = json.substring(expIdx + 6);
            int endIdx = expStr.indexOf(",");
            if (endIdx < 0) {
                endIdx = expStr.indexOf("}");
            }
            long exp = Long.parseLong(expStr.substring(0, endIdx).trim());

            return System.currentTimeMillis() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }
}
