package com.imgvault.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件哈希工具类
 * 支持 SHA-256 和 MD5 双重哈希
 */
public final class FileHashUtil {

    private FileHashUtil() {
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * 计算 SHA-256 哈希
     */
    public static String sha256(InputStream inputStream) throws IOException {
        return hash(inputStream, "SHA-256");
    }

    /**
     * 计算 SHA-256 哈希
     */
    public static String sha256(byte[] data) {
        return hash(data, "SHA-256");
    }

    /**
     * 计算 MD5 哈希
     */
    public static String md5(InputStream inputStream) throws IOException {
        return hash(inputStream, "MD5");
    }

    /**
     * 计算 MD5 哈希
     */
    public static String md5(byte[] data) {
        return hash(data, "MD5");
    }

    private static String hash(InputStream inputStream, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found: " + algorithm, e);
        }
    }

    private static String hash(byte[] data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return bytesToHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found: " + algorithm, e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
