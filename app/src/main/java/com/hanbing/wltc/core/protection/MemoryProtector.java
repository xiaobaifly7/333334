package com.hanbing.wltc.core.protection;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存保护器
 * 用于在内存中安全存储敏感数据，防止内存转储攻击
 */
public final class MemoryProtector {

    // 内存保护是否已激活标志
    private static final AtomicBoolean memoryProtectionActive = new AtomicBoolean(false);
    
    // 安全随机数生成器
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // 内存清理定时器
    private static Timer memoryCleanupTimer;
    
    // 受保护内存区域映射
    private static final Map<String, WeakReference<ProtectedMemory>> protectedAreas = 
            new HashMap<>();
    
    // 清理进行中标志
    private static final AtomicBoolean cleaningInProgress = new AtomicBoolean(false);
    
    // 内存干扰参数
    private static final int MEMORY_NOISE_INTERVAL_MS = 3000;
    private static final int MEMORY_NOISE_SIZE = 1024 * 1024; // 1MB
    
    // 受保护内存区域计数器
    private static final AtomicInteger protectedMemoryCount = new AtomicInteger(0);
    
    // 私有构造函数
    private MemoryProtector() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
    
    /**
     * 初始化内存保护器
     */
    public static void initialize() {
        if (memoryProtectionActive.compareAndSet(false, true)) {
            // 启动内存清理定时器
            startMemoryCleanupTimer();
            
            // 添加JVM关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // 在VM关闭前清理所有受保护内存
                cleanAllProtectedMemory();
                cancelTimers();
            }));
            
            // 检查内存状态是否正常
            if (!checkMemoryState()) {
                // 如果内存状态异常，禁用内存保护
                memoryProtectionActive.set(false);
                throw new SecurityException("内存状态异常");
            }
        }
    }
    
    /**
     * 创建受保护的内存区域
     */
    public static ProtectedMemory createProtectedMemory(int size, String tag) {
        if (!memoryProtectionActive.get()) {
            initialize();
        }
        
        // 在创建前执行安全环境检查
        if (!SecurityGuardian.quickSecurityCheck()) {
            throw new SecurityException("安全环境检查失败");
        }
        
        // 创建新的受保护内存
        ProtectedMemory protectedMemory = new ProtectedMemory(size);
        
        // 将其添加到映射中
        synchronized (protectedAreas) {
            protectedAreas.put(tag + "_" + protectedMemoryCount.incrementAndGet(), 
                    new WeakReference<>(protectedMemory));
        }
        
        return protectedMemory;
    }
    
    /**
     * 检查内存状态
     */
    private static boolean checkMemoryState() {
        try {
            // 获取Java堆内存状态
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 如果内存使用率过高，可能存在内存分析工具或者内存转储攻击正在进行
            double memoryUsageRatio = (double) usedMemory / totalMemory;
            
            // 如果超过90%的内存被使用，认为不安全，不启用内存保护
            if (memoryUsageRatio > 0.9) {
                return false;
            }
            
            // 检查是否存在内存分析工具
            if (areMemoryAnalysisToolsPresent()) {
                return false;
            }
            
            // 尝试执行垃圾回收，确保内存状态准确
            System.gc();
            
            return true;
        } catch (Exception e) {
            // 出现异常表示环境可能不安全
            return false;
        }
    }
    
    /**
     * 检查是否存在内存分析工具
     */
    private static boolean areMemoryAnalysisToolsPresent() {
        // 检查一些常见的内存分析工具类是否存在
        String[] suspiciousClasses = {
            "com.squareup.leakcanary",
            "com.github.moduth.blockcanary",
            "org.eclipse.mat",
            "com.android.tools.profiler",
            "sun.jvm.hotspot.tools",
            "sun.jvm.hotspot.memory"
        };
        
        for (String className : suspiciousClasses) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException ignored) {
                // 未找到该类，继续
            }
        }
        
        return false;
    }
};