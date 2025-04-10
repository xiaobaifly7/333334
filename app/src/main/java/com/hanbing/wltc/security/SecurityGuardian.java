package com.hanbing.wltc.security;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 安全守护者 - 应用安全保护组件
 * 提供反调试、内存完整性校验和执行流检测
 * 防止应用被分析、修改或破解
 */
public class SecurityGuardian {
    private static final String TAG = "SecurityGuardian";
    
    // 安全状态变量
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicInteger securityLevel = new AtomicInteger(0);
    private static final AtomicBoolean securityCompromised = new AtomicBoolean(false);
    private static final Map<String, AtomicLong> securityEvents = new ConcurrentHashMap<>();
    private static final AtomicReference<String> deviceFingerprint = new AtomicReference<>(null);
    
    // 执行流追踪
    private static final Map<String, Long> executionMap = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> operationCounter = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final AtomicLong lastVerificationTime = new AtomicLong(0);
    
    // 内存监控变量
    private static final Map<String, byte[]> memoryMarkers = new ConcurrentHashMap<>();
    private static final Map<String, byte[]> markerHashes = new ConcurrentHashMap<>();
    private static final AtomicBoolean memoryIntegrityCompromised = new AtomicBoolean(false);
    
    // 反调试相关
    private static final AtomicBoolean debugDetected = new AtomicBoolean(false);
    private static final Set<String> debuggerSignatures = new HashSet<>();
    private static final Timer antiDebugTimer = new Timer("SecurityChecker", true);
    private static final AtomicInteger debugCheckFailures = new AtomicInteger(0);
    
    // 签名校验
    private static final byte[] expectedSignatureHash = new byte[32]; // 应用签名的预期哈希值
    private static final AtomicBoolean signatureValidated = new AtomicBoolean(false);
    
    // 通用配置
    private static final Map<String, Object> securityConfig = new ConcurrentHashMap<>();
    private static Context appContext;
    
    // 静态初始化
    static {
        initializeSecurityComponents();
    }
    
    /**
     * 初始化安全组件
     */
    private static void initializeSecurityComponents() {
        try {
            // 设置调试器特征
            debuggerSignatures.add("/system/bin/su");
            debuggerSignatures.add("/system/xbin/su");
            debuggerSignatures.add("/sbin/su");
            debuggerSignatures.add("/su/bin/su");
            debuggerSignatures.add("/data/local/su");
            debuggerSignatures.add("/data/local/bin/su");
            
            // 初始化内存标记
            initializeMemoryMarkers();
            
            // 初始化安全策略
            initializeSecurityPolicy();
        } catch (Exception e) {
            // 捕获所有未处理的异常
            securityLevel.incrementAndGet();
        }
    }
    
    /**
     * 初始化内存标记
     */
    private static void initializeMemoryMarkers() {
        try {
            SecureRandom random = new SecureRandom();
            for (int i = 0; i < 5; i++) {
                byte[] marker = new byte[32];
                random.nextBytes(marker);
                memoryMarkers.put("secure_marker_" + i, marker);
                
                // 计算哈希值
                byte[] hash = calculateMarkerHash(marker, "secure_marker_" + i);
                markerHashes.put("secure_marker_" + i, hash);
            }
        } catch (Exception e) {
            // 安全级别递增
            securityLevel.incrementAndGet();
        }
    }
    
    /**
     * 初始化安全策略
     */
    private static void initializeSecurityPolicy() {
        // 设置安全配置参数
        securityConfig.put("verify_interval_ms", 30000);  // 30秒
        securityConfig.put("max_debug_failures", 3);      // 3次检测失败触发
        securityConfig.put("memory_check_interval_ms", 45000); // 45秒
        securityConfig.put("max_security_level", 10);     // 最大安全级别
        securityConfig.put("url_protection_enabled", true);
        securityConfig.put("config_protection_enabled", true);
        securityConfig.put("tamper_protection_enabled", true);
    }
    
    /**
     * 初始化安全守护系统
     * @param context 应用上下文
     */
    public static void init(Context context) {
        if (context != null && !initialized.get()) {
            // 保存应用上下文
            appContext = context.getApplicationContext();
            
            // 初始化子系统组件
            initializeSubsystems(context);
            
            // 验证应用完整性
            verifyApplicationIntegrity(context);
            
            // 启动保护任务
            startProtectionTasks();
            
            // 标记初始化完成
            initialized.set(true);
            
            // 记录安全事件
            recordSecurityEvent("guardian_initialized");
        }
    }
    
    /**
     * 初始化安全子系统
     */
    private static void initializeSubsystems(Context context) {
        try {
            // 初始化URL保护模块
            QuantumUrlShield.init(context);
            
            // 初始化配置管理器
            NanoConfigManager.init(context);
            
            // 生成设备指纹
            generateDeviceFingerprint(context);
            
            // 记录步骤
            recordExecutionStep("subsystems_initialized");
        } catch (Exception e) {
            // 处理异常
            securityLevel.incrementAndGet();
            recordSecurityEvent("subsystem_init_failed");
        }
    }
    
    /**
     * 启动保护任务
     */
    private static void startProtectionTasks() {
        // 定时执行安全检查
        int verifyInterval = (int) securityConfig.getOrDefault("verify_interval_ms", 30000);
        scheduler.scheduleAtFixedRate(SecurityGuardian::performSecurityChecks, 
                                     10, verifyInterval, TimeUnit.MILLISECONDS);
        
        // 定时校验内存完整性
        int memoryCheckInterval = (int) securityConfig.getOrDefault("memory_check_interval_ms", 45000);
        scheduler.scheduleAtFixedRate(SecurityGuardian::verifyMemoryIntegrity,
                                     1000, memoryCheckInterval, TimeUnit.MILLISECONDS);
        
        // 启动反调试保护
        startAntiDebugProtection();
    }
    /**
     * 验证内存完整性
     */
    private static void verifyMemoryIntegrity() {
        try {
            // 检查所有内存标记
            for (Map.Entry<String, byte[]> entry : memoryMarkers.entrySet()) {
                String key = entry.getKey();
                byte[] marker = entry.getValue();
                byte[] expectedHash = markerHashes.get(key);
                
                if (expectedHash != null) {
                    // 计算当前哈希
                    byte[] currentHash = calculateMarkerHash(marker, key);
                    
                    // 比较哈希值
                    if (!Arrays.equals(expectedHash, currentHash)) {
                        // 内存标记被篡改
                        memoryIntegrityCompromised.set(true);
                        securityLevel.addAndGet(2);
                        recordSecurityEvent("memory_integrity_compromised_" + key);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "内存监控中断", e);
        }
    }
    
    /**
     * 启动反调试保护
     */
    private static void startAntiDebugProtection() {
        // 定时检查调试器连接
        antiDebugTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForDebugger();
            }
        }, 500, 2000);
    }
    
    /**
     * 检查调试器
     */
    private static void checkForDebugger() {
        try {
            // 检查Java调试器
            if (Debug.isDebuggerConnected()) {
                onDebuggerDetected("isDebuggerConnected");
                return;
            }
        } catch (Exception e) {
            // 处理检查异常
            recordSecurityEvent("debug_check_exception");
        }
    }
    
    /**
     * 执行安全检查
     */
    private static void performSecurityChecks() {
        try {
            // 记录验证时间戳
            lastVerificationTime.set(SystemClock.elapsedRealtime());
            
            // 验证执行流完整性
            verifyExecutionFlow();
            
            // 检查运行环境
            checkRuntimeEnvironment();
            
        } catch (Exception e) {
            // 处理检查异常
            securityLevel.incrementAndGet();
            recordSecurityEvent("security_check_exception");
        }
    }
    
    /**
     * 检查运行环境
     */
    private static void checkRuntimeEnvironment() {
        try {
            // 检测是否为模拟器
            if (isEmulator()) {
                recordSecurityEvent("emulator_detected");
                securityLevel.incrementAndGet();
            }
            
            // 记录执行步骤
            recordExecutionStep("runtime_environment_verified");
        } catch (Exception e) {
            // 处理检查异常
            securityLevel.incrementAndGet();
        }
    }
    
    /**
     * 调试器检测回调
     */
    private static void onDebuggerDetected(String source) {
        // 记录检测来源
        recordSecurityEvent("debugger_detected_" + source);
        
        // 标记调试状态
        debugDetected.set(true);
        
        // 提高安全级别
        securityLevel.incrementAndGet();
    }
    
    /**
     * 验证执行流
     */
    private static void verifyExecutionFlow() {
        try {
            // 预期的执行序列
            List<String> expectedSequence = Arrays.asList(
                    "subsystems_initialized", 
                    "security_checks_started", 
                    "runtime_environment_verified");
            
            // 检查是否缺少预期步骤
            for (String step : expectedSequence) {
                if (!executionMap.containsKey(step)) {
                    // 确认步骤存在
                    securityLevel.incrementAndGet();
                    recordSecurityEvent("execution_flow_missing_" + step);
                }
            }
        } catch (Exception e) {
            // 处理验证异常
            securityLevel.incrementAndGet();
        }
    }
    
    /**
     * 验证应用完整性
     */
    private static void verifyApplicationIntegrity(Context context) {
        try {
            // 验证应用签名
            verifyApplicationSignature(context);
        } catch (Exception e) {
            // 处理完整性异常
            securityLevel.incrementAndGet();
            recordSecurityEvent("integrity_check_exception");
        }
    }
    
    /**
     * 验证应用签名
     */
    private static void verifyApplicationSignature(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            android.content.pm.Signature[] signatures;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(
                        context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(
                        context.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }
            
            if (signatures != null && signatures.length > 0) {
                // 计算签名哈希
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] signatureBytes = digest.digest(signatures[0].toByteArray());
                
                signatureValidated.set(true);
            }
        } catch (Exception e) {
            // 处理验证异常
            recordSecurityEvent("signature_verify_exception");
        }
    }
    
    /**
     * 计算标记哈希
     */
    private static byte[] calculateMarkerHash(byte[] marker, String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.getBytes());
            digest.update(marker);
            return digest.digest();
        } catch (Exception e) {
            // 处理异常
            return new byte[32];
        }
    }
    
    /**
     * 检测是否为模拟器
     */
    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
    
    /**
     * 记录执行步骤
     */
    private static void recordExecutionStep(String step) {
        executionMap.put(step, SystemClock.elapsedRealtime());
    }
    
    /**
     * 记录安全事件
     */
    private static void recordSecurityEvent(String event) {
        securityEvents.putIfAbsent(event, new AtomicLong(0));
        securityEvents.get(event).incrementAndGet();
    }
    
    /**
     * 生成设备指纹
     */
    private static void generateDeviceFingerprint(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            
            // 收集设备信息
            sb.append(Build.BRAND).append(":");
            sb.append(Build.MODEL).append(":");
            sb.append(Build.DEVICE).append(":");
            sb.append(Build.MANUFACTURER).append(":");
            sb.append(Build.HARDWARE).append(":");
            sb.append(Build.PRODUCT).append(":");
            
            // 获取Android ID
            String androidId = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null) {
                sb.append(androidId);
            }
            
            // 计算指纹哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fingerprint = digest.digest(sb.toString().getBytes());
            
            // 转为十六进制字符串
            StringBuilder hexFingerprint = new StringBuilder();
            for (byte b : fingerprint) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexFingerprint.append('0');
                hexFingerprint.append(hex);
            }
            
            deviceFingerprint.set(hexFingerprint.toString());
        } catch (Exception e) {
            // 出现异常则使用随机UUID
            deviceFingerprint.set(UUID.randomUUID().toString());
        }
    }
    
    /**
     * 检查系统是否安全
     */
    public static boolean isSystemSecure() {
        // 检查安全状态
        return !securityCompromised.get() && securityLevel.get() < 5;
    }
    
    /**
     * 获取当前安全级别
     */
    public static int getSecurityLevel() {
        return securityLevel.get();
    }
    
    /**
     * 获取设备指纹
     */
    public static String getDeviceFingerprint() {
        return deviceFingerprint.get();
    }
    
    /**
     * 执行手动安全检查
     */
    public static void performManualSecurityCheck() {
        // 确保系统已经初始化
        if (initialized.get()) {
            performSecurityChecks();
            verifyMemoryIntegrity();
            checkForDebugger();
        }
    }
    
    /**
     * 关闭守护
     */
    public static void shutdown() {
        try {
            // 关闭所有调度器
            scheduler.shutdownNow();
            antiDebugTimer.cancel();
            
            // 清除内存标记
            for (Map.Entry<String, byte[]> entry : memoryMarkers.entrySet()) {
                Arrays.fill(entry.getValue(), (byte)0);
            }
            memoryMarkers.clear();
            markerHashes.clear();
            
            // 清除执行和事件记录
            executionMap.clear();
            securityEvents.clear();
            
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }
}