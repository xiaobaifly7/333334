package secure.inject.extreme;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 安全注入器
 * 负责初始化和管理安全组件，并提供安全检查和验证功能
 */
public final class SecurityInjector {
    
    // 初始化标志
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // 注入成功
    private static final AtomicBoolean injectionSuccess = new AtomicBoolean(false);
    
    // 安全管理器引用
    private static final AtomicReference<UltraSecurityManager> securityManager = 
            new AtomicReference<>(null);
    
    // 私有构造器
    private SecurityInjector() {
        throw new UnsupportedOperationException("不允许创建安全注入器实例");
    }
    
    /**
     * 初始化安全组件
     * 包括安全检查和防护措施
     * 
     * @return 初始化是否成功
     */
    public static boolean initialize() {
        // 检查是否已初始化
        if (initialized.get()) {
            return injectionSuccess.get();
        }
        
        // 确保只初始化一次
        if (!initialized.compareAndSet(false, true)) {
            return injectionSuccess.get();
        }
        
        try {
            // 随机延迟，防止时序分析
            long delay = (long) (Math.random() * 200);
            Thread.sleep(delay);
            
            // 执行初始安全检查
            if (!performInitialSecurityCheck()) {
                injectionSuccess.set(false);
                return false;
            }
            
            // 创建安全管理器
            UltraSecurityManager manager = UltraSecurityManager.getInstance();
            securityManager.set(manager);
            
            // 设置安全回调函数
            manager.setSecurityViolationCallback(() -> {
                handleSecurityViolation();
            });
            
            // 标记为成功
            injectionSuccess.set(true);
            
            return true;
        } catch (Throwable t) {
            // 发生异常，标记为失败
            injectionSuccess.set(false);
            
            // 清理敏感数据
            try {
                AntiTamperUtils.handleSecurityException();
            } catch (Throwable ignored) {
                // 忽略异常
            }
            
            return false;
        }
    }
    
    /**
     * 获取安全URL
     * 用于获取经过安全处理的接口地址
     * 
     * @return 安全URL，如安全检查失败返回备用URL
     */
    public static String getSecureUrl() {
        // 确保已初始化
        if (!initialized.get()) {
            boolean success = initialize();
            if (!success) {
                return generateFallbackUrl();
            }
        }
        
        // 检查是否成功
        if (!injectionSuccess.get()) {
            return generateFallbackUrl();
        }
        
        try {
            // 获取安全管理器
            UltraSecurityManager manager = securityManager.get();
            
            // 如果安全性已被破坏，返回备用URL
            if (manager == null || manager.isSecurityCompromised()) {
                return generateFallbackUrl();
            }
            
            // 通过安全管理器获取URL
            return manager.getSecureUrl();
        } catch (Throwable t) {
            // 发生异常时返回备用接口
            return generateFallbackUrl();
        }
    }
    
    /**
     * 执行初始安全检查
     */
    private static boolean performInitialSecurityCheck() {
        try {
            // 检查各项安全
            if (AntiHookDetector.isHookDetected()) {
                return false;
            }
            
            if (!SecurityGuardian.quickSecurityCheck()) {
                return false;
            }
            
            if (AntiTamperUtils.isApplicationTampered()) {
                return false;
            }
            
            // 验证类加载器安全
            if (!validateClassLoader()) {
                return false;
            }
            
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    /**
     * 验证类加载器安全性
     */
    private static boolean validateClassLoader() {
        try {
            // 获取当前类加载器
            ClassLoader currentLoader = SecurityInjector.class.getClassLoader();
            
            // 检查类加载器的类名
            String loaderClassName = currentLoader.getClass().getName();
            
            // 确保是系统级Android类加载器
            boolean isValidLoader = loaderClassName.contains("dalvik.system") || 
                                  loaderClassName.contains("java.lang") ||
                                  loaderClassName.contains("android.app");
            
            if (!isValidLoader) {
                // 发现异常的类加载器，可能被篡改
                return false;
            }
            
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    /**
     * 处理安全违规
     */
    private static void handleSecurityViolation() {
        try {
            // 清空引用
            securityManager.set(null);
            
            // 清理所有敏感数据
            AntiTamperUtils.cleanAllSensitiveData();
            
            // 触发GC回收
            System.gc();
            
        } catch (Throwable ignored) {
            // 忽略异常
        }
    }
    
    /**
     * 生成备用URL
     */
    private static String generateFallbackUrl() {
        // 生成一个基本的备用URL
        return "https://api.example.com/config.json?t=" + System.currentTimeMillis();
    }
    
    /**
     * 检查安全组件是否初始化
     */
    public static boolean isInitialized() {
        return initialized.get() && injectionSuccess.get();
    }
    
    /**
     * 检查安全性是否已被破坏
     */
    public static boolean isSecurityCompromised() {
        if (!initialized.get() || !injectionSuccess.get()) {
            return true;
        }
        
        UltraSecurityManager manager = securityManager.get();
        return manager == null || manager.isSecurityCompromised();
    }
    
    /**
     * 关闭安全组件
     * 清理所有资源和敏感数据
     */
    public static void shutdown() {
        try {
            // 获取安全管理器
            UltraSecurityManager manager = securityManager.get();
            
            // 关闭安全管理器
            if (manager != null) {
                manager.shutdown();
            }
            
            // 清理引用
            securityManager.set(null);
            
            // 触发GC
            System.gc();
            
        } catch (Throwable ignored) {
            // 忽略异常
        }
    }
} 