package com.hanbing.wltc.core.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Base64 as AndroidBase64;

/**
 * 量子URL保护盾
 * 用于保护关键API请求，防止被中间人攻击或篡改
 */
public class QuantumUrlShield {
    
    private static final String TAG = "QuantumUrlShield";
    private static final String SECRET_KEY = "QU4NT_uM$h13LD_k3Y";
    private static final String DELIMITER = "__QUANTUM__";
    private static final int TIME_TOLERANCE_MS = 60000; // 1分钟时间容差
    
    /**
     * 生成受保护的URL
     * @param originalUrl 原始URL
     * @return 添加了防篡改签名的URL
     */
    public static String protect(String originalUrl) {
        try {
            // 添加时间戳参数
            String timestampedUrl = addParameter(originalUrl, "_ts", String.valueOf(System.currentTimeMillis()));
            
            // 添加随机数参数，用于防止重放攻击
            String nonce = generateNonce(8);
            String urlWithNonce = addParameter(timestampedUrl, "_nonce", nonce);
            
            // 计算签名
            String signature = calculateSignature(urlWithNonce);
            
            // 添加签名参数
            return addParameter(urlWithNonce, "_sig", signature);
        } catch (Exception e) {
            // 出错时返回原始URL，确保功能可降级
            return originalUrl;
        }
    }
    
    /**
     * 验证URL是否有效（未被篡改且在有效期内）
     * @param protectedUrl 受保护的URL
     * @return 如果URL有效则返回true
     */
    public static boolean verify(String protectedUrl) {
        try {
            // 提取签名参数
            String signature = extractParameter(protectedUrl, "_sig");
            if (signature == null || signature.isEmpty()) {
                return false;
            }
            
            // 移除签名参数以计算预期签名
            String urlWithoutSig = removeParameter(protectedUrl, "_sig");
            String expectedSignature = calculateSignature(urlWithoutSig);
            
            // 验证签名
            if (!signature.equals(expectedSignature)) {
                return false;
            }
            
            // 验证时间戳
            String timestamp = extractParameter(protectedUrl, "_ts");
            if (timestamp == null || timestamp.isEmpty()) {
                return false;
            }
            
            // 检查时间戳是否在允许范围内
            long ts = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            return Math.abs(currentTime - ts) <= TIME_TOLERANCE_MS;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算URL的签名
     * @param url 需要签名的URL
     * @return 签名字符串
     */
    private static String calculateSignature(String url) throws Exception {
        // 组合URL和密钥
        String dataToSign = url + DELIMITER + SECRET_KEY;
        
        // 使用SHA-256计算哈希
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(dataToSign.getBytes(StandardCharsets.UTF_8));
        
        // 转换为Base64字符串并截取前16个字符（为了URL长度考虑）
        String base64Hash = AndroidBase64.encodeToString(hashBytes, AndroidBase64.NO_WRAP);
        if (base64Hash.length() > 16) {
            base64Hash = base64Hash.substring(0, 16);
        }
        
        return base64Hash;
    }
    
    /**
     * 生成指定长度的随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    private static String generateNonce(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 向URL添加参数
     * @param url 原始URL
     * @param paramName 参数名
     * @param paramValue 参数值
     * @return 添加参数后的URL
     */
    private static String addParameter(String url, String paramName, String paramValue) {
        if (url.contains("?")) {
            return url + "&" + paramName + "=" + paramValue;
        } else {
            return url + "?" + paramName + "=" + paramValue;
        }
    }
    
    /**
     * 从URL中提取参数值
     * @param url URL
     * @param paramName 参数名
     * @return 参数值，如果不存在则返回null
     */
    private static String extractParameter(String url, String paramName) {
        Pattern pattern = Pattern.compile(paramName + "=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 从URL中移除指定参数
     * @param url URL
     * @param paramName 参数名
     * @return 移除参数后的URL
     */
    private static String removeParameter(String url, String paramName) {
        // 移除 ?param=value 或 &param=value
        String result = url.replaceAll("[\?&]" + paramName + "=[^&]*", "");
        
        // 如果第一个参数被移除，需要修复URL格式
        if (result.contains("&") && !result.contains("?")) {
            result = result.replaceFirst("&", "?");
        }
        
        return result;
    }
}