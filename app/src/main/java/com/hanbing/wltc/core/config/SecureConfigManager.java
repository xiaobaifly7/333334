package com.hanbing.wltc.core.config;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 安全配置管理器
 * 负责配置文件的解密、验证和内存保护功能
 */
public final class SecureConfigManager {
    
    private static final String TAG = "SecureConfigManager";
    
    // GCM参数
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    // 缓存配置
    private static final AtomicReference<String> cachedConfig = new AtomicReference<>(null);
    
    // 受保护内存配置存储
    private static MemoryProtector.ProtectedMemory configMemory;
    
    // 配置是否有效标志
    private static volatile boolean configValid = false;
    
    // 配置最后更新时间
    private static volatile long lastConfigUpdateTime = 0;
    
    // 配置缓存有效期限制(2小时)
    private static final long CONFIG_CACHE_DURATION = 2 * 60 * 60 * 1000;
    
    // 私有构造器防止实例化
    private SecureConfigManager() {
        throw new UnsupportedOperationException("不允许创建安全配置管理器实例");
    }
    
    /**
     * 初始化管理器
     */
    public static void initialize() {
        // 创建受保护内存区域
        if (configMemory == null) {
            try {
                MemoryProtector.initialize();
                configMemory = MemoryProtector.createProtectedMemory(8192, "secure_config");
            } catch (Exception e) {
                Log.e(TAG, "初始化内存保护失败", e);
            }
        }
    }
    
    /**
     * 获取设备ID分组的哈希值
     */
    public static String getDeviceGroupHash() {
        try {
            byte[] deviceId = DeviceIdentity.generateDeviceFingerprint();
            return bytesToHex(
                    MessageDigest.getInstance("SHA-256").digest(deviceId)
            ).substring(0, 12);
        } catch (Exception e) {
            Log.e(TAG, "生成设备组哈希失败", e);
            return "default";
        }
    }
    
    /**
     * 获取配置文件路径
     */
    public static String[] getConfigFilePaths() {
        String groupHash = getDeviceGroupHash();
        
        // 返回两个备用配置文件路径，提高容错性
        // 文件名基于设备指纹生成，确保每个设备唯一（避免共享缓存攻击）
        return new String[] {
            "/configs/" + groupHash + "/" + getSecureFileName1() + ".dat",
            "/configs/" + groupHash + "/" + getSecureFileName2() + ".dat"
        };
    }
    
    /**
     * 获取安全文件名称一 (主配置文件名)
     */
    private static String getSecureFileName1() {
        try {
            byte[] deviceId = DeviceIdentity.generateDeviceFingerprint();
            byte[] nameKey = Arrays.copyOf(deviceId, 16);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(nameKey);
            digest.update("filename1".getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            return bytesToHex(hash).substring(0, 16);
        } catch (Exception e) {
            // 返回一个默认值 (与UltraSecureUrlProvider保持同步)
            return "a1b2c3d4e5f6g7h8";
        }
    }
    
    /**
     * 获取安全文件名称二 (备份文件名)
     */
    private static String getSecureFileName2() {
        try {
            byte[] deviceId = DeviceIdentity.generateDeviceFingerprint();
            byte[] nameKey = Arrays.copyOf(deviceId, 16);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(nameKey);
            digest.update("filename2".getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            return bytesToHex(hash).substring(0, 16);
        } catch (Exception e) {
            // 返回一个默认值 (与UltraSecureUrlProvider保持同步)
            return "h8g7f6e5d4c3b2a1";
        }
    }
    
    /**
     * 解密配置
     * 
     * @param encryptedContent 加密的配置内容字符串
     * @return 解密后的JSON配置字符串
     * @throws SecurityException 解密失败或安全校验不通过时抛出
     */
    public static String decryptConfig(String encryptedContent) throws SecurityException {
        // 首先安全检查
        if (!SecurityGuardian.quickSecurityCheck()) {
            throw new SecurityException("安全检查未通过");
        }
        
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            throw new SecurityException("加密内容为空");
        }
        
        try {
            // 混淆执行流
            AntiTamperUtils.obfuscateExecutionFlow();
            
            // 提取JSON
            String dataBase64 = extractJsonValue(encryptedContent, "data");
            String ivBase64 = extractJsonValue(encryptedContent, "iv");
            String metaBase64 = extractJsonValue(encryptedContent, "meta");
            
            if (dataBase64 == null || ivBase64 == null || metaBase64 == null) {
                throw new SecurityException("无效的加密格式");
            }
            
            // 解码
            byte[] encryptedBytes = Base64.decode(dataBase64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
            byte[] metaBytes = Base64.decode(metaBase64, Base64.NO_WRAP);
            
            // 提取元数据信息
            String metaString = new String(metaBytes, StandardCharsets.UTF_8);
            String[] metaParts = metaString.split("\\|");
            if (metaParts.length != 2) {
                throw new SecurityException("元数据格式不正确");
            }
            
            String metaJson = metaParts[0];
            String metaChecksum = metaParts[1];
            
            // 从设备ID生成解密密钥
            byte[] deviceId = DeviceIdentity.generateDeviceFingerprint();
            byte[] decryptionKey = deriveKeyFromDeviceId(deviceId);
            
            // 验证元数据的完整性校验和
            String calculatedChecksum = calculateHmac(metaJson, decryptionKey);
            if (!calculatedChecksum.equals(metaChecksum)) {
                throw new SecurityException("元数据完整性校验失败");
            }
            
            // 检查时间戳和过期信息
            long timestamp = extractJsonLong(metaJson, "timestamp");
            long expiry = extractJsonLong(metaJson, "expiry");
            long currentTime = System.currentTimeMillis();
            
            if (currentTime > expiry) {
                throw new SecurityException("配置已过期");
            }
            
            // 解密数据
            byte[] decryptedBytes = decryptWithAesGcm(encryptedBytes, decryptionKey, iv);
            String decryptedConfig = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            // 将配置存储在受保护内存中
            storeConfigInProtectedMemory(decryptedConfig);
            
            // 更新缓存和状态
            cachedConfig.set(decryptedConfig);
            configValid = true;
            lastConfigUpdateTime = System.currentTimeMillis();
            
            return decryptedConfig;
        } catch (Exception e) {
            configValid = false;
            throw new SecurityException("配置解密或验证失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取安全配置内容
     */
    public static String getSecureConfig() {
        // 检查缓存是否在有效期内
        if (configValid && cachedConfig.get() != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastConfigUpdateTime <= CONFIG_CACHE_DURATION) {
                return cachedConfig.get();
            }
        }
        
        // 尝试从受保护内存中读取
        if (configMemory != null) {
            try {
                byte[] buffer = new byte[8192];
                configMemory.read(buffer, 0, buffer.length);
                
                // 找到字符串结束位置
                int end = 0;
                while (end < buffer.length && buffer[end] != 0) {
                    end++;
                }
                
                if (end > 0) {
                    String config = new String(buffer, 0, end, StandardCharsets.UTF_8);
                    if (config.length() > 10) { // 简单的有效性检查
                        cachedConfig.set(config);
                        configValid = true;
                        return config;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "从受保护内存读取配置失败", e);
            }
        }
        
        // 如果上述方法都失败，返回最后缓存的配置
        return cachedConfig.get();
    }
    
    /**
     * 将配置存储在受保护内存中
     */
    private static void storeConfigInProtectedMemory(String config) {
        if (configMemory != null && config != null) {
            try {
                byte[] configBytes = config.getBytes(StandardCharsets.UTF_8);
                configMemory.write(configBytes, 0, Math.min(configBytes.length, 8192));
            } catch (Exception e) {
                Log.e(TAG, "写入受保护内存失败", e);
            }
        }
    }
    
    /**
     * 清除配置
     */
    public static void clearConfig() {
        configValid = false;
        cachedConfig.set(null);
        
        if (configMemory != null) {
            try {
                configMemory.clear();
            } catch (Exception e) {
                Log.e(TAG, "清除内存失败", e);
            }
        }
    }
    
    /**
     * 从JSON中提取字符串值
     */
    private static String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * 从JSON中提取长整型数值
     */
    private static long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(json);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return 0;
    }
    
    /**
     * 使用AES-GCM解密
     */
    private static byte[] decryptWithAesGcm(byte[] encryptedData, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * 计算HMAC-SHA256
     */
    private static String calculateHmac(String data, byte[] key) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
        hmac.init(keySpec);
        byte[] hmacBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }
    
    /**
     * 从设备ID派生加密密钥
     */
    private static byte[] deriveKeyFromDeviceId(byte[] deviceId) throws Exception {
        // 使用设备ID生成SHA-256哈希作为基础密钥
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] baseKey = digest.digest(deviceId);
        
        // 创建最终密钥，将基础密钥的前半部分复制过来
        byte[] derivedKey = new byte[32]; // 256位密钥
        System.arraycopy(baseKey, 0, derivedKey, 0, 16);
        
        // 计算后半部分
        byte[] salt = "ConfigSecuritySalt".getBytes(StandardCharsets.UTF_8);
        digest.reset();
        digest.update(baseKey);
        digest.update(salt);
        byte[] secondHalf = digest.digest();
        System.arraycopy(secondHalf, 0, derivedKey, 16, 16);
        
        return derivedKey;
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}