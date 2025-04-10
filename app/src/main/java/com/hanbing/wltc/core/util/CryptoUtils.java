package com.hanbing.wltc.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加密工具类
 * 提供各种加密、解密和哈希函数
 */
public final class CryptoUtils {
    
    // 加密算法常量定义
    private static final String ALGORITHM_AES = "AES";
    private static final String TRANSFORMATION_AES = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM_SHA256 = "SHA-256";
    
    // 初始化向量种子
    private static final byte[] IV_SEED = {
        (byte) 0x67, (byte) 0x3A, (byte) 0x0F, (byte) 0x58,
        (byte) 0x94, (byte) 0xC2, (byte) 0xE1, (byte) 0xD5,
        (byte) 0x8D, (byte) 0x2B, (byte) 0x4A, (byte) 0x16,
        (byte) 0x7E, (byte) 0xF3, (byte) 0xA0, (byte) 0xB9
    };
    
    // 私有构造器
    private CryptoUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }
    
    /**
     * 计算SHA-256哈希值
     * 
     * @param input 输入数据
     * @return 哈希计算结果
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM_SHA256);
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
    
    /**
     * 计算SHA-256哈希并转换为十六进制字符串
     * 
     * @param input 待哈希的字符串
     * @return 十六进制表示的哈希值
     */
    public static String sha256Hex(String input) {
        byte[] hash = sha256(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    /**
     * 密钥派生函数 - 简化的PBKDF2实现
     * 
     * @param seed 种子
     * @param salt 盐值
     * @param iterations 迭代次数
     * @return 派生的密钥
     */
    public static byte[] deriveKey(byte[] seed, byte[] salt, int iterations) {
        byte[] result = new byte[seed.length];
        System.arraycopy(seed, 0, result, 0, seed.length);
        
        for (int i = 0; i < iterations; i++) {
            // 混合种子和盐值
            byte[] mixed = new byte[result.length + salt.length];
            System.arraycopy(result, 0, mixed, 0, result.length);
            System.arraycopy(salt, 0, mixed, result.length, salt.length);
            
            // 计算哈希
            result = sha256(mixed);
            
            // 额外的混淆步骤
            if (i % 2 == 0) {
                // 偶数轮次翻转
                for (int j = 0; j < result.length / 2; j++) {
                    byte temp = result[j];
                    result[j] = result[result.length - j - 1];
                    result[result.length - j - 1] = temp;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 加密数据
     * 
     * @param data 待加密的数据
     * @param key 加密密钥
     * @return 加密后的数据
     */
    public static byte[] encrypt(byte[] data, byte[] key) {
        try {
            // 调整密钥大小为2字节(256位)
            byte[] adjustedKey = adjustKeySize(key, 32);
            
            // 生成IV
            byte[] iv = generateIV(adjustedKey);
            
            // 初始化加密器
            SecretKeySpec secretKeySpec = new SecretKeySpec(adjustedKey, ALGORITHM_AES);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            // 加密
            byte[] encryptedData = cipher.doFinal(data);
            
            // 将IV附加到结果前
            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }
    
    /**
     * 解密数据
     * 
     * @param encryptedData 加密数据
     * @param key 加密密钥
     * @return 解密后的数据
     */
    public static byte[] decrypt(byte[] encryptedData, byte[] key) {
        try {
            // 调整密钥大小为2字节(256位)
            byte[] adjustedKey = adjustKeySize(key, 32);
            
            // 提取IV（前16字节）
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16);
            
            // 提取加密数据
            byte[] actualEncryptedData = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);
            
            // 初始化解密器
            SecretKeySpec secretKeySpec = new SecretKeySpec(adjustedKey, ALGORITHM_AES);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            
            // 解密
            return cipher.doFinal(actualEncryptedData);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
    
    /**
     * 调整密钥大小
     */
    private static byte[] adjustKeySize(byte[] key, int length) {
        if (key.length == length) {
            return key;
        }
        
        byte[] result = new byte[length];
        
        if (key.length < length) {
            // 如果密钥太短，重复使用
            int pos = 0;
            while (pos < length) {
                int remaining = length - pos;
                int toCopy = Math.min(remaining, key.length);
                System.arraycopy(key, 0, result, pos, toCopy);
                pos += toCopy;
            }
        } else {
            // 如果密钥太长，使用哈希
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(ALGORITHM_SHA256);
                digest.update(key);
                byte[] hash = digest.digest();
                System.arraycopy(hash, 0, result, 0, Math.min(hash.length, length));
            } catch (NoSuchAlgorithmException e) {
                // 如果哈希不可用，截断
                System.arraycopy(key, 0, result, 0, length);
            }
        }
        
        return result;
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 生成初始化向量
     */
    private static byte[] generateIV(byte[] key) {
        // 基于提供的密钥和固定的IV种子生成初始化向量
        try {
            ByteArrayCombiner combiner = new ByteArrayCombiner();
            combiner.add(IV_SEED);
            combiner.add(key);
            
            byte[] combinedData = combiner.toByteArray();
            byte[] hash = sha256(combinedData);
            
            // 从哈希提取16字节作为IV
            byte[] iv = new byte[16];
            System.arraycopy(hash, 0, iv, 0, 16);
            
            return iv;
        } finally {
            // 清理
            Arrays.fill(IV_SEED, (byte) 0);
        }
    }
    
    /**
     * 字节数组组合工具
     */
    private static class ByteArrayCombiner {
        private byte[][] arrays = new byte[10][];
        private int count = 0;
        private int totalLength = 0;
        
        /**
         * 添加字节数组
         */
        public void add(byte[] array) {
            ensureCapacity(count + 1);
            arrays[count++] = array;
            totalLength += array.length;
        }
        
        /**
         * 确保容量足够
         */
        private void ensureCapacity(int minCapacity) {
            if (minCapacity > arrays.length) {
                int newCapacity = Math.max(arrays.length * 2, minCapacity);
                arrays = Arrays.copyOf(arrays, newCapacity);
            }
        }
        
        /**
         * 转换为字节数组
         */
        public byte[] toByteArray() {
            byte[] result = new byte[totalLength];
            int destPos = 0;
            for (int i = 0; i < count; i++) {
                byte[] array = arrays[i];
                System.arraycopy(array, 0, result, destPos, array.length);
                destPos += array.length;
            }
            return result;
        }
    }
}