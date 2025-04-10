package com.hanbing.wltc.core.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import secure.inject.extreme.tools.ConfigEncryptionTool;

/**
 * 配置安全性测试类
 * 用于测试配置的加密、解密和内存保护功能
 */
public class ConfigSecurityTest {
    
    private static final String TAG = "ConfigSecurityTest";
    private static final String TEST_CONFIG = "{\"version\":\"1.0\",\"features\":{\"testFeature\":true,\"premium\":false},\"settings\":{\"timeout\":30,\"retries\":3}}";
    private static final String TEST_DEVICE_ID = "test_device_123456";
    private static final String TEST_DEVICE_GROUP = "test_group_alpha";
    private static final String OUTPUT_DIR = "./test_output";
    
    public static void main(String[] args) {
        try {
            System.out.println("开始执行配置安全性测试...");
            
            // 创建输出目录
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 创建测试配置文件
            String configFilePath = OUTPUT_DIR + "/test_config.json";
            Files.write(Paths.get(configFilePath), TEST_CONFIG.getBytes());
            System.out.println("测试配置文件已创建于: " + configFilePath);
            
            // 测试加密功能
            testConfigEncryption(configFilePath);
            
            // 测试解密与验证功能
            testConfigDecryption();
            
            System.out.println("所有测试已完成");
            
        } catch (Exception e) {
            System.err.println("测试执行过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试配置加密功能
     */
    private static void testConfigEncryption(String configFilePath) {
        try {
            System.out.println("\n===== 测试配置加密 =====");
            
            // 准备加密工具参数
            String[] toolArgs = {
                configFilePath,
                TEST_DEVICE_GROUP,
                OUTPUT_DIR + "/encrypted"
            };
            
            // 执行加密
            ConfigEncryptionTool.main(toolArgs);
            
            // 检查加密后的输出文件
            File encryptedDir = new File(OUTPUT_DIR + "/encrypted");
            if (encryptedDir.exists() && encryptedDir.isDirectory()) {
                File[] files = encryptedDir.listFiles();
                if (files != null && files.length > 0) {
                    System.out.println("加密成功，输出目录中有 " + files.length + " 个文件:");
                    for (File file : files) {
                        System.out.println("- " + file.getName());
                    }
                } else {
                    System.err.println("加密失败，输出目录为空");
                }
            } else {
                System.err.println("加密失败，输出目录不存在");
            }
            
        } catch (Exception e) {
            System.err.println("加密测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试配置解密与验证功能
     */
    private static void testConfigDecryption() {
        System.out.println("\n===== 测试配置解密与验证=====");
        
        try {
            // 设置设备ID用于解密
            DeviceIdentity.setOverrideDeviceId(TEST_DEVICE_ID);
            
            // 设置路径
            System.setProperty("secure.config.dir", OUTPUT_DIR + "/encrypted");
            
            // 初始化安全配置管理器
            SecureConfigManager.initialize();
            System.out.println("安全配置管理器已初始化");
            
            // 获取解密后的配置
            String decryptedConfig = SecureConfigManager.getDecryptedConfig();
            if (decryptedConfig != null && !decryptedConfig.isEmpty()) {
                System.out.println("解密结果:");
                System.out.println(decryptedConfig);
                
                // 验证解密后的配置是否与原始配置匹配
                if (isConfigValid(decryptedConfig)) {
                    System.out.println("验证成功，解密后的配置包含所有预期的元素");
                } else {
                    System.err.println("验证失败，解密后的配置没有包含所有预期的元素");
                }
            } else {
                System.err.println("解密失败，无法获取配置内容");
            }
            
            // 测试内存保护
            testMemoryProtection(decryptedConfig);
            
        } catch (Exception e) {
            System.err.println("解密测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            DeviceIdentity.setOverrideDeviceId(null);
            System.clearProperty("secure.config.dir");
        }
    }
    
    /**
     * 测试内存保护功能
     */
    private static void testMemoryProtection(String config) {
        System.out.println("\n===== 测试内存保护功能 =====");
        
        try {
            // 创建内存保护器
            MemoryProtector memoryProtector = new MemoryProtector();
            
            // 创建受保护内存
            long memoryId = memoryProtector.createProtectedMemory(config.getBytes());
            System.out.println("受保护内存已创建，ID: " + memoryId);
            
            // 从受保护内存读取
            byte[] protectedData = memoryProtector.readMemory(memoryId);
            String protectedConfig = new String(protectedData);
            System.out.println("已从受保护内存读取数据");
            
            // 验证数据完整性
            if (protectedConfig.equals(config)) {
                System.out.println("数据完整性验证成功，内存保护机制工作正常");
            } else {
                System.err.println("数据完整性验证失败，内存保护机制不正常");
            }
            
            // 清除内存
            memoryProtector.clearMemory(memoryId);
            System.out.println("受保护内存已清除");
            
            // 尝试读取已清除的内存
            try {
                byte[] clearedData = memoryProtector.readMemory(memoryId);
                System.err.println("警告：能够读取已被清除的内存，保护机制可能失效");
            } catch (Exception e) {
                System.out.println("预期的异常：无法读取已清除的内存(" + e.getMessage() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("内存保护测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证配置内容是否有效
     */
    private static boolean isConfigValid(String decryptedConfig) {
        try {
            // 检查配置中是否包含所有预期的键值对
            return decryptedConfig.contains("\"version\":\"1.0\"") &&
                   decryptedConfig.contains("\"testFeature\":true") &&
                   decryptedConfig.contains("\"premium\":false") &&
                   decryptedConfig.contains("\"timeout\":30") &&
                   decryptedConfig.contains("\"retries\":3");
        } catch (Exception e) {
            System.err.println("配置验证异常: " + e.getMessage());
            return false;
        }
    }
}