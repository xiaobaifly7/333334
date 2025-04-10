package com.hanbing.wltc.core.network;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * URL提取器
 * 用于从加密的片段中安全地提取和重组原始URL
 */
public final class UrlExtractor {
    
    // 错误消息常量
    private static final String ERROR_INVALID_INPUT = "输入参数无效";
    private static final String ERROR_SECURITY_CHECK_FAILED = "安全环境检查失败";
    private static final String ERROR_DECRYPTION_FAILED = "解密过程失败";
    
    // 启用敏感数据安全擦除（防止内存转储攻击）
    private static final boolean ENABLE_SECURE_WIPE = true;
    
    // 私有构造函数
    private UrlExtractor() {
        throw new UnsupportedOperationException("URL提取器不允许实例化");
    }
    
    /**
     * 从加密片段中提取URL
     *
     * @param encryptedFragments 加密的URL片段
     * @param fragmentOrder 片段顺序密钥
     * @param masterKey 主密钥
     * @return 解密后的URL
     * @throws SecurityException 安全检查失败或解密失败
     */
    public static String extractUrl(
            String[] encryptedFragments, 
            int[] fragmentOrder, 
            String masterKey) throws SecurityException {
        
        // 快速安全检查
        if (!SecurityGuardian.quickSecurityCheck()) {
            throw new SecurityException(ERROR_SECURITY_CHECK_FAILED);
        }
        
        // 输入验证
        if (encryptedFragments == null || fragmentOrder == null || 
            encryptedFragments.length == 0 || masterKey == null || masterKey.isEmpty()) {
            throw new IllegalArgumentException(ERROR_INVALID_INPUT);
        }
        
        // 获取主密钥字节数组
        byte[] masterKeyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
        
        try {
            // 获取设备指纹
            byte[] deviceFingerprint = DeviceIdentity.generateDeviceFingerprint();
            
            // 生成设备特定密钥
            byte[] deviceSpecificKey = CryptoUtils.generateDeviceSpecificKey(
                    masterKeyBytes, deviceFingerprint);
            
            // 解密并合并
            return decryptFragmentsAndCombine(encryptedFragments, fragmentOrder, deviceSpecificKey);
        } catch (Exception e) {
            throw new SecurityException(ERROR_DECRYPTION_FAILED, e);
        } finally {
            // 清除密钥内存
            if (ENABLE_SECURE_WIPE) {
                Arrays.fill(masterKeyBytes, (byte) 0);
            }
        }
    }
    
    /**
     * 解密片段并组合成URL
     */
    private static String decryptFragmentsAndCombine(
            String[] encryptedFragments, 
            int[] fragmentOrder, 
            byte[] deviceSpecificKey) {
        
        // 检查安全环境
        if (!SecurityGuardian.isSecureEnvironment()) {
            return generateFakeUrl();
        }
        
        // 验证片段和顺序长度匹配
        int fragmentCount = encryptedFragments.length;
        if (fragmentCount != fragmentOrder.length) {
            return generateFakeUrl();
        }
        
        // 生成基于时间的盐值
        byte[] timeSalt = generateTimeSalt();
        
        // 按顺序解密并组合片段
        StringBuilder urlBuilder = new StringBuilder();
        
        for (int i = 0; i < fragmentCount; i++) {
            // 获取当前位置应使用的片段索引
            int fragmentIndex = fragmentOrder[i] % fragmentCount;
            
            // 获取片段
            String encryptedFragment = encryptedFragments[fragmentIndex];
            if (encryptedFragment == null || encryptedFragment.isEmpty()) {
                continue;
            }
            
            try {
                // 转换为字节数组（十六进制字符串转换）
                byte[] encryptedBytes = CryptoUtils.hexToBytes(encryptedFragment);
                
                // 多层解密
                byte[] decryptedBytes = CryptoUtils.multiLayerDecrypt(
                        encryptedBytes, deviceSpecificKey, timeSalt);
                
                // 转换为字符串并附加到结果
                String fragmentText = new String(decryptedBytes, StandardCharsets.UTF_8);
                urlBuilder.append(fragmentText);
                
                // 擦除解密后的数据
                if (ENABLE_SECURE_WIPE) {
                    Arrays.fill(decryptedBytes, (byte) 0);
                }
            } catch (Exception e) {
                // 任何解密错误返回假URL
                return generateFakeUrl();
            }
        }
        
        // 最终安全性再次验证
        if (!SecurityGuardian.performDeepSecurityCheck()) {
            return generateFakeUrl();
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * 生成时间盐值
     */
    private static byte[] generateTimeSalt() {
        // 计算当前日期的时间组件
        // 这里使用日期级别的时间戳，以便同一天内使用相同的盐值
        long timeComponent = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
        
        // 转换为字节数组
        byte[] timeBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            timeBytes[i] = (byte) (timeComponent >> (i * 8));
        }
        
        return timeBytes;
    }
    
    /**
     * 生成假URL（当安全检查失败时使用）
     */
    private static String generateFakeUrl() {
        // 生成一个看起来合理但实际无效的URL，避免返回明显错误
        return "https://example.com/resources/config_" + 
               System.currentTimeMillis() % 1000 + ".json";
    }
    
    /**
     * 高安全性URL提取（包含时间戳验证和增强密钥派生）
     */
    public static String extractUrlSecure(
            String[] encryptedFragments, 
            int[] fragmentOrder,
            String masterKey,
            long timestamp) throws SecurityException {
        
        // 执行深度安全检查
        if (!SecurityGuardian.performDeepSecurityCheck()) {
            throw new SecurityException(ERROR_SECURITY_CHECK_FAILED);
        }
        
        // 输入验证
        if (encryptedFragments == null || fragmentOrder == null || 
            encryptedFragments.length == 0 || masterKey == null || masterKey.isEmpty()) {
            throw new IllegalArgumentException(ERROR_INVALID_INPUT);
        }
        
        // 解密过程
        try {
            // 获取主密钥字节数组
            byte[] masterKeyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
            
            // 获取设备指纹
            byte[] deviceFingerprint = DeviceIdentity.generateDeviceFingerprint();
            
            // 生成设备特定密钥
            byte[] deviceSpecificKey = CryptoUtils.generateDeviceSpecificKey(
                    masterKeyBytes, deviceFingerprint);
            
            // 时间戳验证
            long currentTime = System.currentTimeMillis();
            long timeDifference = Math.abs(currentTime - timestamp);
            
            // 验证时间戳是否合理
            if (timeDifference > 24 * 60 * 60 * 1000) {
                throw new SecurityException("时间戳验证失败");
            }
            
            // 基于提供的时间戳生成盐值
            byte[] timeBasedSalt = generateTimeSaltFromTimestamp(timestamp);
            
            // 派生增强密钥
            byte[] enhancedKey = CryptoUtils.deriveKey(deviceSpecificKey, timeBasedSalt, 2000);
            
            // 安全解密片段
            String result = secureDecryptFragments(encryptedFragments, fragmentOrder, enhancedKey);
            
            // 验证结果有效性
            if (result == null || !isValidHttpUrl(result)) {
                throw new SecurityException("URL格式验证失败");
            }
            
            return result;
        } catch (Exception e) {
            throw new SecurityException(ERROR_DECRYPTION_FAILED, e);
        }
    }
    
    /**
     * 安全解密片段
     */
    private static String secureDecryptFragments(
            String[] encryptedFragments,
            int[] fragmentOrder,
            byte[] enhancedKey) {
        
        ByteArrayCombiner resultCombiner = new ByteArrayCombiner();
        byte[] fragmentSalt = CryptoUtils.sha256(enhancedKey);
        
        try {
            for (int i = 0; i < fragmentOrder.length; i++) {
                int fragmentIndex = fragmentOrder[i] % encryptedFragments.length;
                String encryptedFragment = encryptedFragments[fragmentIndex];
                
                // 为每个片段生成唯一的盐值以防止模式分析
                fragmentSalt = CryptoUtils.sha256(fragmentSalt);
                
                // 解密片段
                byte[] encryptedBytes = CryptoUtils.hexToBytes(encryptedFragment);
                byte[] decryptedBytes = CryptoUtils.multiLayerDecrypt(
                        encryptedBytes, enhancedKey, fragmentSalt);
                
                // 添加到结果
                resultCombiner.add(decryptedBytes);
                
                // 清理内存
                Arrays.fill(decryptedBytes, (byte) 0);
            }
            
            // 获取完整的结果
            byte[] combinedBytes = resultCombiner.toByteArray();
            String result = new String(combinedBytes, StandardCharsets.UTF_8);
            
            // 清理
            Arrays.fill(combinedBytes, (byte) 0);
            resultCombiner.clear();
            
            return result;
        } finally {
            // 确保在所有情况下清理敏感数据
            Arrays.fill(fragmentSalt, (byte) 0);
            resultCombiner.clear();
        }
    }
    
    /**
     * 从时间戳生成盐值
     */
    private static byte[] generateTimeSaltFromTimestamp(long timestamp) {
        // 提取日期组件
        long timeComponent = timestamp / (24 * 60 * 60 * 1000);
        
        // 转换为字节数组
        byte[] timeBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            timeBytes[i] = (byte) (timeComponent >> (i * 8));
        }
        
        return timeBytes;
    }
    
    /**
     * 验证URL是否为有效的HTTP链接
     */
    private static boolean isValidHttpUrl(String url) {
        return url != null && 
              (url.startsWith("http://") || url.startsWith("https://")) &&
               url.length() > 10 && 
               url.contains(".");
    }
}