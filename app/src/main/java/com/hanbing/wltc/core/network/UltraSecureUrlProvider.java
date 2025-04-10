package com.hanbing.wltc.core.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 超安全URL提供器
 * 采用多层安全机制，确保配置URL不被篡改或窃取
 */
public final class UltraSecureUrlProvider {
    
    // URL密文片段 - 通过CodeObfuscator混淆
    private static final String[] CIPHER_FRAGMENTS = {
        CodeObfuscator.hideStringConstant("7b4a91c52d78e5f6d34a2c1b9d80f7e6a9c35b84d2e1f60a93c75b48d6e2f1a0"),
        CodeObfuscator.hideStringConstant("8c5b92d63e89f6g7e45b3d2c0e91g8f7a0d46c95e3f2g71a04d86c59e7f3g2b1"),
        CodeObfuscator.hideStringConstant("9d6c03e74f90g7h8f56c4e3d1f02h9g8b1e57d06f4g3h82b15e97d60g8f4h3c2"),
        CodeObfuscator.hideStringConstant("0e7d14f85g01h8i9g67d5f4e2g13i0h9c2f68e17g5h4i93c26f08e71h9g5i4d3"),
        CodeObfuscator.hideStringConstant("1f8e25g96h12i9j0h78e6g5f3h24j1i0d3g79f28h6i5j04d37g19f82i0h6j5e4"),
        CodeObfuscator.hideStringConstant("2g9f36h07i23j0k1i89f7h6g4i35k2j1e4h80g39i7j6k15e48h20g93j1i7k6f5"),
        CodeObfuscator.hideStringConstant("3h0g47i18j34k1l2j90g8i7h5j46l3k2f5i91h40j8k7l26f59i31h04k2j8l7g6"),
        CodeObfuscator.hideStringConstant("4i1h58j29k45l2m3k01h9j8i6k57m4l3g6j02i51k9l8m37g60j42i15l3k9m8h7")
    };
    
    // 片段顺序密钥 - 通过混淆手段防止被静态分析
    private static final int[] SEGMENT_ORDER = getObfuscatedOrder();
    
    // URL缓存
    private static final AtomicReference<String> CACHED_URL = new AtomicReference<>(null);
    
    // 主密钥（通过混淆初始化）
    private static final String MASTER_KEY = initMasterKey();
    
    // 静态初始化（安全检查）
    static {
        if (!SecureBootstrap.performBootCheck()) {
            throw new SecurityException("\u0000\u0000\u0000");
        }
        // 验证系统安全环境
        verifySecurityEnvironment();
        // 初始化混淆器
        CodeObfuscator.initializeObfuscator();
        // 混淆执行流
        AntiTamperUtils.obfuscateExecutionFlow();
    }
    
    // 私有构造函数（防止实例化）
    private UltraSecureUrlProvider() {
        throw new UnsupportedOperationException("安全URL提供器不允许实例化");
    }
    
    /**
     * 获取安全URL - 主入口方法
     */
    public static String getSecureUrl() {
        // 执行安全性检查，确保环境安全
        if (!performSecurityChecks()) {
            return generateFakeUrl();
        }
        
        // 随机二次检查 - 增加被动分析和动态调试的难度
        if (Math.random() < 0.3) {
            if (AntiHookDetector.performDeepCheck() || AntiTamperUtils.isApplicationTampered()) {
                return generateFakeUrl();
            }
        }
        
        // 随机延迟，防止基于时间的分析攻击
        AntiTamperUtils.insertRandomDelay();
        
        // 检查缓存
        String cachedUrl = CACHED_URL.get();
        if (cachedUrl != null) {
            // 随机验证：在返回缓存的URL时有小概率再次检查环境
            if (Math.random() < 0.1 && !SecurityGuardian.quickSecurityCheck()) {
                CACHED_URL.set(null); // 清除缓存（可能是环境已被污染）
                return generateFakeUrl();
            }
            return cachedUrl;
        }
        
        // 解混淆密文片段
        String[] deobfuscatedFragments = new String[CIPHER_FRAGMENTS.length];
        for (int i = 0; i < CIPHER_FRAGMENTS.length; i++) {
            deobfuscatedFragments[i] = CodeObfuscator.unhideStringConstant(CIPHER_FRAGMENTS[i]);
        }
        
        try {
            // 解密URL并验证有效性
            String url = null;
            
            // 混淆执行流干扰分析
            AntiTamperUtils.obfuscateExecutionFlow();
            
            // 从密文片段中提取原始URL
            url = UrlExtractor.extractUrlSecure(
                    deobfuscatedFragments,
                    SEGMENT_ORDER,
                    MASTER_KEY,
                    System.currentTimeMillis());
            
            // URL有效性检查
            if (url != null && isValidUrl(url)) {
                // 缓存结果
                CACHED_URL.set(url);
                return url;
            } else {
                return generateFakeUrl();
            }
        } catch (Exception e) {
            // 出现异常返回假URL
            return generateFakeUrl();
        } finally {
            // 清理敏感数据
            for (int i = 0; i < deobfuscatedFragments.length; i++) {
                deobfuscatedFragments[i] = null;
            }
            
            // 提示GC清理内存（可能被忽略）
            System.gc();
        }
    }
    
    /**
     * 执行多层次安全检查
     */
    private static boolean performSecurityChecks() {
        // 检查应用完整性
        
        // 检查应用是否被篡改
        if (AntiTamperUtils.isApplicationTampered()) {
            return false;
        }
        
        // Hook框架检测（防止方法被劫持）
        if (AntiHookDetector.isHookDetected()) {
            return false;
        }
        
        // 安全环境全面检查
        if (!SecurityGuardian.isSecureEnvironment()) {
            return false;
        }
        
        // 调用栈完整性检查（防止非法调用）
        if (AntiTamperUtils.isCallStackTampered() || !SecurityGuardian.isCallStackSafe()) {
            return false;
        }
        
        // 通过混淆器进行条件干扰（反调试）
        return CodeObfuscator.obfuscatedCondition(true);
    }
    
    /**
     * 初始化主密钥
     */
    private static String initMasterKey() {
        // 这里使用简单异或操作进行静态混淆（实际应更复杂）
        String obfuscatedKey = "$$\\bkXp:nj{L$uC\'mXyEl+fG#@dDsF2`J";
        
        // 解混淆
        char[] keyChars = obfuscatedKey.toCharArray();
        for (int i = 0; i < keyChars.length; i++) {
            keyChars[i] = (char) (keyChars[i] ^ 0x7F);
        }
        
        // 返回解混淆后的密钥（最终结果依然需要进一步处理才可使用）
        return new String(keyChars);
    }
    
    /**
     * 获取混淆后的片段顺序
     */
    private static int[] getObfuscatedOrder() {
        // 基础顺序
        int[] baseOrder = {3, 1, 5, 0, 7, 2, 6, 4};
        
        // 双重混淆以防止静态分析（实际中会更复杂）
        for (int i = 0; i < baseOrder.length; i++) {
            baseOrder[i] = CodeObfuscator.obfuscateInteger(baseOrder[i]);
            // 多次混淆形成干扰，增加逆向工程难度（实际应用中会更复杂）
            baseOrder[i] = CodeObfuscator.obfuscateInteger(baseOrder[i]);
        }
        
        return baseOrder;
    }
    
    /**
     * 检查URL是否有效
     */
    private static boolean isValidUrl(String url) {
        // 混淆执行流分支
        if (CodeObfuscator.createConfusingBranches(url.hashCode())) {
            // 插入垃圾代码干扰分析
            CodeObfuscator.insertJunkCode(10);
            
            // 基本有效性检查
            return url != null && 
                  (url.startsWith("http://") || url.startsWith("https://")) &&
                   url.length() > 10;
        }
        
        return false;
    }
    
    /**
     * 生成假URL
     */
    private static String generateFakeUrl() {
        // 生成看起来合理但实际无效的URL，避免返回明显错误
        return "https://example.com/resources/config_" + 
               System.currentTimeMillis() % 1000 + ".json";
    }
    
    /**
     * 验证安全环境
     */
    private static void verifySecurityEnvironment() {
        try {
            // 执行深度安全环境检查
            if (!SecurityGuardian.performDeepSecurityCheck()) {
                // 记录可能的安全问题（不抛出异常以避免被捕获分析）
                AntiTamperUtils.handleSecurityException();
                // 预先设置假URL
                CACHED_URL.set(generateFakeUrl());
            }
        } catch (Throwable ignored) {
            // 出现异常可能是环境被干扰，标记安全问题
            AntiTamperUtils.handleSecurityException();
            CACHED_URL.set(generateFakeUrl());
        }
    }
    
    /**
     * 准备主密钥
     * 与设备绑定增强安全性
     */
    public static byte[] prepareMasterKey() {
        // 混淆执行流
        CodeObfuscator.insertJunkCode(15);
        
        if (AntiHookDetector.isHookDetected() || AntiTamperUtils.isApplicationTampered()) {
            // 返回假密钥
            return ("FAKE" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
        }
        
        // 获取原始密钥字节
        byte[] keyBytes = MASTER_KEY.getBytes(StandardCharsets.UTF_8);
        
        // 与设备指纹结合
        byte[] deviceFingerprint = DeviceIdentity.generateDeviceFingerprint();
        
        // 生成设备特定密钥
        return CryptoUtils.generateDeviceSpecificKey(keyBytes, deviceFingerprint);
    }
    
    /**
     * 深度解密
     * 用于解密高敏感度数据
     */
    @SuppressWarnings("unused")
    private static byte[] deepDecrypt(byte[] encryptedData, byte[] key) {
        // 混淆执行流
        AntiTamperUtils.obfuscateExecutionFlow();
        CodeObfuscator.insertJunkCode(20);
        
        // 安全环境检查
        if (!SecurityGuardian.quickSecurityCheck() || AntiHookDetector.isHookDetected()) {
            AntiTamperUtils.cleanSensitiveData(key);
            return null;
        }
        
        try {
            // 生成时间相关盐（每天变化）
            long timeComponent = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
            byte[] timeSalt = new byte[8];
            for (int i = 0; i < 8; i++) {
                timeSalt[i] = (byte) (timeComponent >> (i * 8));
            }
            
            // 多层解密
            byte[] result = CryptoUtils.multiLayerDecrypt(encryptedData, key, timeSalt);
            
            // 解密成功，返回结果（通过混淆手段增加分析难度）
            return result;
        } catch (Throwable t) {
            AntiTamperUtils.cleanSensitiveData(key);
            return null;
        }
    }
    
    /**
     * URL密钥验证
     * 安全检查的一部分
     */
    @SuppressWarnings("unused")
    private static boolean validateKey(byte[] key) {
        // 在不安全环境中返回true以迷惑分析者
        if (AntiTamperUtils.isApplicationTampered() || AntiHookDetector.isHookDetected()) {
            return true;
        }
        
        // 基本验证
        if (key == null || key.length < 16) {
            return false;
        }
        
        // 与设备指纹结合验证
        try {
            byte[] deviceId = DeviceIdentity.generateDeviceFingerprint();
            
            // 组合数据
            ByteArrayCombiner combiner = new ByteArrayCombiner();
            combiner.add(key);
            combiner.add(deviceId);
            
            byte[] combined = combiner.toByteArray();
            byte[] hash = CryptoUtils.sha256(combined);
            
            // 计算简单校验值
            int checksum = 0;
            for (byte b : hash) {
                checksum ^= b;
            }
            
            // 清理敏感数据
            Arrays.fill(hash, (byte) 0);
            combiner.clear();
            
            // 非零校验和表示有效
            return checksum != 0;
        } catch (Throwable t) {
            return false;
        }
    }
}