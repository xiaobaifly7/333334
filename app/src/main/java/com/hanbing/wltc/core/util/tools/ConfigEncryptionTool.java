package com.hanbing.wltc.core.util.tools;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;

/**
 * 配置加密工具
 * 用于加密和签名配置文件
 * 支持按设备或用户分组加密
 */
public class ConfigEncryptionTool {
    // AES-GCM加密参数
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    // JSON模板格式
    private static final String JSON_TEMPLATE = "{\"version\":\"1.0\",\"timestamp\":\"%d\",\"data\":\"%s\"}";
    
    // 输出JSON格式
    private static final String OUTPUT_TEMPLATE = "{\"config\":\"%s\",\"signature\":\"%s\"}";
    
    public static void main(String[] args) {
        // 参数检查
        if (args.length != 3) {
            System.out.println("用法: ConfigEncryptionTool <配置文件> <设备ID/用户ID> <输出目录>");
            return;
        }
        
        try {
            // 读取配置文件
            String configJson = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(args[0])), StandardCharsets.UTF_8);
            System.out.println("配置文件: " + args[0] + " (" + configJson.length() + " 字节)");
            
            // 设备ID/用户标识
            String deviceId = args[1];
            System.out.println("设备/用户ID: " + deviceId);
            
            // 计算分组哈希值 (用于文件分组)
            String groupHash = calculateGroupHash(deviceId);
            String outputDir = args[2] + File.separator + groupHash;
            new File(outputDir).mkdirs();
            
            // 生成加密密钥
            SecretKey key = generateKey(deviceId);
            
            // 加密配置
            String encryptedConfig = encryptConfig(configJson, key);
            
            // 生成签名
            String signature = generateSignature(encryptedConfig, key);
            
            // 生成输出JSON
            String outputJson = String.format(OUTPUT_TEMPLATE, encryptedConfig, signature);
            
            // 写入输出文件
            String outputFile = outputDir + File.separator + "config.json";
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(outputJson.getBytes(StandardCharsets.UTF_8));
            }
            
            System.out.println("配置已保存到: " + outputFile);
            
        } catch (Exception e) {
            System.err.println("错误发生: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String calculateGroupHash(String deviceId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("计算分组哈希失败", e);
        }
    }
    
    private static SecretKey generateKey(String deviceId) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }
    
    private static String encryptConfig(String config, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(config.getBytes(StandardCharsets.UTF_8));
        
        // 合并IV和加密数据
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    private static String generateSignature(String data, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }
}