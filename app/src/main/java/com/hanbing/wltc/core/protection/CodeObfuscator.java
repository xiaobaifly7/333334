package com.hanbing.wltc.core.protection;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 代码混淆工具
 * 提供运行时的字符串加密和解密功能
 */
public class CodeObfuscator {

    private static final String TAG = "CodeObfuscator";
    
    // 默认加密密钥 (仅示例，实际应用中应更加复杂且安全存储)
    private static final String DEFAULT_KEY = "H3rBi6n7W9tL1cK4";
    
    // 初始化向量
    private static final String DEFAULT_IV = "A5xB8zC3dE2fG1hI";
    
    // 字符串常量池 (预加密的常用字符串)
    private static final String[] STRING_POOL = {
        "YXBpLnNlcnZlci5jb20=",           // "api.server.com"
        "YXV0aG9yaXphdGlvbg==",           // "authorization"
        "Y29udGVudC10eXBl",              // "content-type"
        "YXBwbGljYXRpb24vanNvbg==",       // "application/json"
        "Y29ubmVjdGlvbi10aW1lb3V0",       // "connection-timeout"
        "dXNlci1hZ2VudA==",               // "user-agent"
        "c2VjdXJpdHkta2V5",              // "security-key"
    };
    
    /**
     * 解密字符串常量池中的字符串
     * @param index 字符串索引
     * @return 解密后的字符串
     */
    public static String getStringFromPool(int index) {
        if (index < 0 || index >= STRING_POOL.length) {
            return "";
        }
        try {
            return new String(Base64.decode(STRING_POOL[index], Base64.DEFAULT), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 解密AES加密的字符串
     * @param encryptedText Base64编码的加密字符串
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, DEFAULT_KEY, DEFAULT_IV);
    }
    
    /**
     * 解密AES加密的字符串（使用自定义密钥）
     * @param encryptedText Base64编码的加密字符串
     * @param key 16字节的AES密钥
     * @param iv 16字节的初始化向量
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String key, String iv) {
        try {
            // 将Base64编码的密文解码为字节数组
            byte[] encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
            
            // 准备密钥和初始化向量
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // 解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            // 转换为字符串并返回
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 加密字符串（用于开发阶段生成加密字符串）
     * @param plainText 明文
     * @return Base64编码的加密字符串
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, DEFAULT_KEY, DEFAULT_IV);
    }
    
    /**
     * 加密字符串（使用自定义密钥）
     * @param plainText 明文
     * @param key 16字节的AES密钥
     * @param iv 16字节的初始化向量
     * @return Base64编码的加密字符串
     */
    public static String encrypt(String plainText, String key, String iv) {
        try {
            // 准备密钥和初始化向量
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // 加密
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Base64编码并返回
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 混淆整数值
     * @param value 原始整数值
     * @return 混淆后的整数
     */
    public static int obfuscateInt(int value) {
        // 简单的整数混淆算法（仅示例）
        return ~(value ^ 0xABCDEF12);
    }
    
    /**
     * 还原混淆的整数值
     * @param obfuscatedValue 混淆后的整数
     * @return 原始整数值
     */
    public static int deobfuscateInt(int obfuscatedValue) {
        // 还原混淆的整数
        return ~obfuscatedValue ^ 0xABCDEF12;
    }
}