package com.hanbing.wltc.core.network;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 安全URL生成器
 * 负责生成带有安全签名的URL
 */
public class SecureUrlGenerator {

    private static final String TAG = "SecureUrlGenerator";
    
    // 密钥常量
    private static final String SECRET_KEY = "hb7j3k9p2q5z8x1c4v7";
    
    // URL分隔符
    private static final String URL_SEPARATOR = "&";
    
    /**
     * 生成安全URL
     * @param baseUrl 基础URL
     * @param params 参数字符串
     * @return 带有安全签名的URL
     */
    public static String generateSecureUrl(String baseUrl, String params) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return null;
        }
        
        // 获取时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // 生成随机盐值
        String salt = generateSalt();
        
        // 计算签名
        String signature = calculateSignature(params, timestamp, salt);
        
        // 构建最终URL
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        
        // 添加参数
        if (params != null && !params.isEmpty()) {
            if (baseUrl.contains("?")) {
                urlBuilder.append(URL_SEPARATOR);
            } else {
                urlBuilder.append("?");
            }
            urlBuilder.append(params);
        }
        
        // 添加时间戳
        if (urlBuilder.toString().contains("?")) {
            urlBuilder.append(URL_SEPARATOR);
        } else {
            urlBuilder.append("?");
        }
        urlBuilder.append("t=").append(timestamp);
        
        // 添加盐值
        urlBuilder.append(URL_SEPARATOR).append("s=").append(salt);
        
        // 添加签名
        urlBuilder.append(URL_SEPARATOR).append("sign=").append(signature);
        
        return urlBuilder.toString();
    }
    
    /**
     * 生成随机盐值
     */
    private static String generateSalt() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 计算签名
     */
    private static String calculateSignature(String params, String timestamp, String salt) {
        try {
            // 构建签名原文
            StringBuilder signatureBase = new StringBuilder();
            signatureBase.append(params == null ? "" : params);
            signatureBase.append(timestamp);
            signatureBase.append(salt);
            signatureBase.append(SECRET_KEY);
            
            // 使用SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(signatureBase.toString().getBytes(StandardCharsets.UTF_8));
            
            // Base64编码
            return Base64.encodeToString(hashBytes, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (Exception e) {
            return "";
        }
    }
}