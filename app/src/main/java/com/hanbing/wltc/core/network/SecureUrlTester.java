package com.hanbing.wltc.core.network;

/**
 * 安全URL测试工具
 * 用于测试和验证安全URL提取和生成功能
 */
public class SecureUrlTester {
    
    // 加密的URL片段 (实际部署时这些会放在不同位置并且混淆)
    private static final String[] ENCRYPTED_FRAGMENTS = {
        "7b4a91c52d78e5f6d34a2c1b9d80f7e6a9c35b84d2e1f60a93c75b48d6e2f1a0",
        "8c5b92d63e89f6g7e45b3d2c0e91g8f7a0d46c95e3f2g71a04d86c59e7f3g2b1",
        "9d6c03e74f90g7h8f56c4e3d1f02h9g8b1e57d06f4g3h82b15e97d60g8f4h3c2",
        "0e7d14f85g01h8i9g67d5f4e2g13i0h9c2f68e17g5h4i93c26f08e71h9g5i4d3",
        "1f8e25g96h12i9j0h78e6g5f3h24j1i0d3g79f28h6i5j04d37g19f82i0h6j5e4",
        "2g9f36h07i23j0k1i89f7h6g4i35k2j1e4h80g39i7j6k15e48h20g93j1i7k6f5",
        "3h0g47i18j34k1l2j90g8i7h5j46l3k2f5i91h40j8k7l26f59i31h04k2j8l7g6",
        "4i1h58j29k45l2m3k01h9j8i6k57m4l3g6j02i51k9l8m37g60j42i15l3k9m8h7"
    };
    
    // 片段顺序密钥
    private static final int[] FRAGMENT_ORDER = {3, 1, 5, 0, 7, 2, 6, 4};
    
    // 主密钥
    private static final String MASTER_KEY = "LDWe7gwK2hMvnC$zTNku8F#YpAxZ6Vb9";
    
    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        try {
            // 测试标准URL提取
            testStandardUrlExtraction();
            
            // 测试安全URL提取
            testSecureUrlExtraction();
            
            // 测试URL生成工具
            testUrlGenerationTool();
            
        } catch (Exception e) {
            System.out.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试标准URL提取
     */
    private static void testStandardUrlExtraction() {
        System.out.println("=== 测试标准URL提取 ===");
        
        try {
            String url = UrlExtractor.extractUrl(
                    ENCRYPTED_FRAGMENTS,
                    FRAGMENT_ORDER,
                    MASTER_KEY);
            
            System.out.println("提取的URL: " + url);
        } catch (SecurityException e) {
            System.out.println("安全错误: " + e.getMessage());
        }
    }
    
    /**
     * 测试安全URL提取
     */
    private static void testSecureUrlExtraction() {
        System.out.println("\n=== 测试安全URL提取 ===");
        
        try {
            long timestamp = System.currentTimeMillis();
            
            String url = UrlExtractor.extractUrlSecure(
                    ENCRYPTED_FRAGMENTS,
                    FRAGMENT_ORDER,
                    MASTER_KEY,
                    timestamp);
            
            System.out.println("安全提取的URL: " + url);
        } catch (SecurityException e) {
            System.out.println("安全错误: " + e.getMessage());
        }
    }
    
    /**
     * 测试URL生成工具
     */
    private static void testUrlGenerationTool() {
        System.out.println("\n=== 测试URL生成工具 ===");
        
        // 注意：此功能仅用于开发和测试环境，不应在生产环境中使用
        // 用于生成加密片段
        
        try {
            String originalUrl = "https://example.com/api/config.json";
            generateEncryptedFragments(originalUrl, MASTER_KEY);
        } catch (Exception e) {
            System.out.println("生成加密片段时出错: " + e.getMessage());
        }
    }
    
    /**
     * 生成加密的片段
     */
    private static void generateEncryptedFragments(String url, String masterKey) {
        System.out.println("原始URL: " + url);
        
        // 将URL分割成多个片段
        int fragmentSize = (url.length() + 7) / 8;  // 大约均分
        String[] fragments = new String[8];
        
        for (int i = 0; i < 8; i++) {
            int start = i * fragmentSize;
            int end = Math.min(start + fragmentSize, url.length());
            
            if (start < url.length()) {
                fragments[i] = url.substring(start, end);
            } else {
                fragments[i] = "";  // 填充空字符串
            }
        }
        
        // 模拟设备指纹
        byte[] deviceFingerprint = "test-device-fingerprint".getBytes();
        
        // 加密每个片段
        System.out.println("\n加密的URL片段：");
        byte[] masterKeyBytes = masterKey.getBytes();
        byte[] deviceSpecificKey = CryptoUtils.generateDeviceSpecificKey(
                masterKeyBytes, deviceFingerprint);
        
        byte[] timeSalt = new byte[8];  // 用零填充的盐值
        
        for (int i = 0; i < fragments.length; i++) {
            try {
                byte[] fragmentData = fragments[i].getBytes();
                byte[] encryptedData = CryptoUtils.multiLayerEncrypt(
                        fragmentData, deviceSpecificKey, timeSalt);
                
                String hexString = CryptoUtils.bytesToHex(encryptedData);
                System.out.println("片段 " + i + ": " + hexString);
            } catch (Exception e) {
                System.out.println("片段 " + i + " 加密失败: " + e.getMessage());
            }
        }
        
        System.out.println("\n片段顺序:");
        System.out.print("private static final int[] FRAGMENT_ORDER = {");
        for (int i = 0; i < 8; i++) {
            System.out.print(i + (i < 7 ? ", " : ""));
        }
        System.out.println("};");
    }
}