package com.hanbing.wltc.core.security;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

/**
 * 超级安全管理器
 * 提供应用安全功能的总体管控
 */
public final class UltraSecurityManager {
    
    // 单例实例
    private static volatile UltraSecurityManager INSTANCE;
    
    // 是否检测到安全违规
    private final AtomicBoolean securityViolationDetected = new AtomicBoolean(false);
    
    // 安全检查计数器
    private final AtomicInteger checkCounter = new AtomicInteger(0);
    
    // 安全检查调度器
    private ScheduledExecutorService securityScheduler;
    
    // 安全检查间隔时间(毫秒)
    private static final long SECURITY_CHECK_INTERVAL_MS = 2500;
    
    // 安全检查时间抖动范围(毫秒)
    private static final long SECURITY_CHECK_JITTER_MS = 1500;
    
    // 最大安全失败次数
    private static final int MAX_SECURITY_FAILURES = 3;
    
    // 安全违规回调
    private Runnable securityViolationCallback;
    
    // 受保护内存区域 - 用于存储安全URL
    private MemoryProtector.ProtectedMemory urlMemory;
    private boolean memoryProtectionEnabled = false;
    private boolean networkSecurityEnabled = false;
    
    // 安全失败计数
    private final AtomicInteger securityFailureCount = new AtomicInteger(0);
    
    // 私有构造函数
    private UltraSecurityManager() {
        // 执行初始安全检查
        if (!initialSecurityCheck()) {
            handleSecurityViolation();
            return;
        }
        
        // 初始化安全调度器
        initializeSecurityScheduler();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized UltraSecurityManager getInstance() {
        // 双重检查锁定
        if (INSTANCE == null) {
            // 执行一些混淆操作
            AntiTamperUtils.obfuscateExecutionFlow();
            
            synchronized (UltraSecurityManager.class) {
                if (INSTANCE == null) {
                    // 确保环境安全
                    if (AntiHookDetector.isHookDetected() || !SecurityGuardian.isSecureEnvironment()) {
                        INSTANCE = null;
                        throw new SecurityException("安全环境检查失败");
                    } else {
                        INSTANCE = new UltraSecurityManager();
                    }
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 初始安全检查
     */
    private boolean initialSecurityCheck() {
        // 检查是否有Hook
        if (AntiHookDetector.isHookDetected()) {
            return false;
        }
        
        if (!SecurityGuardian.isSecureEnvironment()) {
            return false;
        }
        
        if (AntiTamperUtils.isApplicationTampered()) {
            return false;
        }
        
        // 执行一些混淆操作
        AntiTamperUtils.obfuscateExecutionFlow();
        CodeObfuscator.insertJunkCode(15);
        
        // 检查调用栈是否被篡改
        if (AntiTamperUtils.isCallStackTampered()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 初始化安全调度器
     */
    private void initializeSecurityScheduler() {
        securityScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName(CodeObfuscator.hideStringConstant("SecurityMonitor"));
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        
        // 调度第一次安全检查
        scheduleNextSecurityCheck();
    }
    
    /**
     * 调度下一次安全检查
     */
    private void scheduleNextSecurityCheck() {
        if (securityScheduler == null || securityScheduler.isShutdown()) {
            return;
        }
        
        // 基础间隔时间 +/- 随机抖动
        long baseInterval = SECURITY_CHECK_INTERVAL_MS;
        long jitter = (long) (Math.random() * SECURITY_CHECK_JITTER_MS);
        
        // 50%概率增加抖动，50%概率减少抖动
        long nextDelay = Math.random() > 0.5 ? 
                baseInterval + jitter : Math.max(baseInterval - jitter, 500);
        
        securityScheduler.schedule(() -> {
            try {
                performSecurityCheck();
            } finally {
                // 递归调度下一次检查
                scheduleNextSecurityCheck();
            }
        }, nextDelay, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 执行安全检查
     */
    private void performSecurityCheck() {
        try {
            // 验证当前线程名称
            String threadName = Thread.currentThread().getName();
            String expectedName = CodeObfuscator.unhideStringConstant(
                    CodeObfuscator.hideStringConstant("SecurityMonitor"));
            
            if (!threadName.equals(expectedName)) {
                handleSecurityViolation();
                return;
            }
            
            // 默认假设检查通过
            boolean checkResult = true;
            
            // 随机插入混淆代码
            if (Math.random() < 0.3) {
                AntiTamperUtils.obfuscateExecutionFlow();
            }
            
            // 随机选择一种安全检查
            double random = Math.random();
            
            if (random < 0.33) {
                // 检查Hook
                checkResult = !AntiHookDetector.performDeepCheck();
            } else if (random < 0.66) {
                // 检查安全环境
                checkResult = SecurityGuardian.isSecureEnvironment();
            } else {
                // 检查应用完整性
                checkResult = !AntiTamperUtils.isApplicationTampered();
            }
            
            // 每5次执行一次全面检查
            if (checkCounter.incrementAndGet() % 5 == 0) {
                checkResult = checkResult && 
                             !AntiHookDetector.isHookDetected() && 
                              SecurityGuardian.isSecureEnvironment() && 
                             !AntiTamperUtils.isApplicationTampered();
            }
            
            // 处理检查结果
            if (!checkResult) {
                incrementFailureCount();
            } else {
                resetFailureCount();
            }
        } catch (Throwable t) {
            // 捕获到异常也算安全检查失败
            incrementFailureCount();
        }
    }
    
    /**
     * 获取安全URL - 使用内存保护或加密存储
     */
    public String getSecureUrl() {
        // 如果已检测到安全违规，返回降级URL
        if (securityViolationDetected.get()) {
            return UltraSecureUrlProvider.getSecureUrl(); // 返回降级URL
        }
        
        // 随机执行快速安全检查
        if (Math.random() < 0.2) {
            performQuickSecurityCheck();
        }
        
        // 如果启用了内存保护，从受保护内存中读取URL
        if (memoryProtectionEnabled && urlMemory != null) {
            try {
                // 尝试从受保护内存读取URL
                String cachedUrl = readUrlFromProtectedMemory();
                if (cachedUrl != null && !cachedUrl.isEmpty()) {
                    return cachedUrl;
                }
            } catch (Exception e) {
                // 失败时禁用内存保护
                memoryProtectionEnabled = false;
            }
        }
        
        // 从提供者获取URL
        String url = UltraSecureUrlProvider.getSecureUrl();
        
        // 如果启用了内存保护，将URL存储到受保护内存
        if (memoryProtectionEnabled && url != null && !url.isEmpty()) {
            try {
                storeUrlInProtectedMemory(url);
            } catch (Exception e) {
                // 忽略存储错误
            }
        }
        
        return url;
    }
    
    /**
     * 执行快速安全检查
     */
    private void performQuickSecurityCheck() {
        try {
            // 检查是否在主线程中调用
            String threadName = Thread.currentThread().getName();
            if (threadName.equals("main")) {
                // 在主线程上不执行耗时检查
                if (!SecurityGuardian.quickSecurityCheck()) {
                    incrementFailureCount();
                }
            } else {
                // 在非主线程上可以执行更全面的检查
                if (!SecurityGuardian.isSecureEnvironment() || 
                    AntiHookDetector.isHookDetected()) {
                    incrementFailureCount();
                }
            }
        } catch (Exception e) {
            // 捕获到异常也算安全检查失败
            incrementFailureCount();
        }
    }
    
    /**
     * 设置安全违规回调
     * @param callback 安全违规时要执行的回调
     */
    public void setSecurityViolationCallback(Runnable callback) {
        this.securityViolationCallback = callback;
        
        // 如果已经检测到安全违规，立即执行回调
        if (securityViolationDetected.get() && callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                // 忽略回调执行错误
            }
        }
    }
    
    /**
     * 增加安全失败计数
     */
    private void incrementFailureCount() {
        int count = securityFailureCount.incrementAndGet();
        if (count >= MAX_SECURITY_FAILURES) {
            handleSecurityViolation();
        }
    }
    
    /**
     * 重置安全失败计数
     */
    private void resetFailureCount() {
        securityFailureCount.set(0);
    }
    
    /**
     * 处理安全违规
     */
    private void handleSecurityViolation() {
        // 设置安全违规标志
        if (securityViolationDetected.compareAndSet(false, true)) {
            // 执行一些混淆操作
            AntiTamperUtils.obfuscateExecutionFlow();
            
            // 关闭安全调度器
            if (securityScheduler != null && !securityScheduler.isShutdown()) {
                securityScheduler.shutdownNow();
            }
            
            // 清除受保护内存
            if (urlMemory != null) {
                urlMemory.clear();
                urlMemory = null;
            }
            
            // 执行回调
            if (securityViolationCallback != null) {
                try {
                    securityViolationCallback.run();
                } catch (Exception e) {
                    // 忽略回调执行错误
                }
            }
        }
    }
    
    /**
     * 启用内存保护
     */
    public void enableMemoryProtection() {
        if (securityViolationDetected.get()) {
            return;
        }
        
        try {
            // 创建受保护内存区域
            if (urlMemory == null) {
                urlMemory = MemoryProtector.allocateProtectedMemory(256);
            }
            
            memoryProtectionEnabled = true;
        } catch (Exception e) {
            memoryProtectionEnabled = false;
        }
    }
    
    /**
     * 启用网络安全
     */
    public void enableNetworkSecurity() {
        if (securityViolationDetected.get()) {
            return;
        }
        
        try {
            // 初始化网络安全组件
            NetworkSecurityInitializer.initialize();
            
            // 设置标志
            networkSecurityEnabled = true;
        } catch (Exception e) {
            networkSecurityEnabled = false;
        }
    }
    
    /**
     * 验证应用完整性
     * @param challenge 挑战值
     * @return 响应值
     */
    public byte[] verifyIntegrity(byte[] challenge) {
        if (securityViolationDetected.get() || challenge == null) {
            return null;
        }
        
        try {
            // 执行快速安全检查
            if (!SecurityGuardian.quickSecurityCheck()) {
                incrementFailureCount();
                return null;
            }
            
            // 混淆执行流程
            if (Math.random() < 0.5) {
                AntiTamperUtils.obfuscateExecutionFlow();
            }
            
            // 计算响应
            IntegrityVerifier verifier = IntegrityVerifier.getInstance();
            byte[] response = verifier.processChallenge(challenge);
            
            // 对结果进行编码
            if (response != null) {
                response = xorBytes(response, CodeObfuscator.getSecretKey());
            }
            
            return response;
        } catch (Exception e) {
            incrementFailureCount();
            return null;
        }
    }
    
    /**
     * 检查安全性是否已被破坏
     */
    public boolean isSecurityCompromised() {
        // 执行快速安全检查
        if (!SecurityGuardian.quickSecurityCheck()) {
            securityViolationDetected.set(true);
        }
        
        return securityViolationDetected.get();
    }
    
    @Override
    protected void finalize() throws Throwable {
        // 清理资源
        if (securityScheduler != null && !securityScheduler.isShutdown()) {
            securityScheduler.shutdownNow();
        }
        
        // 清除受保护内存
        if (urlMemory != null) {
            urlMemory.clear();
            urlMemory = null;
        }
        
        super.finalize();
    }
    
    /**
     * 从受保护内存读取URL
     */
    private String readUrlFromProtectedMemory() {
        if (urlMemory == null) {
            return null;
        }
        
        byte[] data = urlMemory.read();
        if (data == null || data.length == 0) {
            return null;
        }
        
        return new String(data, StandardCharsets.UTF_8);
    }
    
    /**
     * 将URL存储到受保护内存
     */
    private void storeUrlInProtectedMemory(String url) {
        if (urlMemory == null || url == null) {
            return;
        }
        
        byte[] data = url.getBytes(StandardCharsets.UTF_8);
        urlMemory.write(data);
    }
    
    /**
     * 对字节数组进行XOR操作
     */
    private byte[] xorBytes(byte[] data, byte[] key) {
        if (data == null || key == null || key.length == 0) {
            return data;
        }
        
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        
        return result;
    }
}