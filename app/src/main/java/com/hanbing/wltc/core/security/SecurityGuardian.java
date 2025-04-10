package com.hanbing.wltc.core.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.Debug;

/**
 * 安全守护组件
 * 提供多层次安全检测和环境完整性验证
 */
public final class SecurityGuardian {
    
    // 安全性受损标志
    private static final AtomicBoolean securityCompromised = new AtomicBoolean(false);
    
    // 安全检查是否已执行标志
    private static final AtomicBoolean securityCheckPerformed = new AtomicBoolean(false);
    
    // 调试包名列表
    private static final String[] DEBUG_PACKAGES = {
        "android.support.multidex.MultiDex",
        "com.android.tools.fd.runtime",
        "com."+ CodeObfuscator.hideStringConstant("android.tools.ir"),
        "com.android.internal.util.WithFramework",
        "com.android.dex.Dex",
        "dalvik.system.VMDebug",
        "de.robv.android.xposed",
        "com.saurik.substrate"
    };
    
    // 模拟器特征文件列表
    private static final String[] EMULATOR_FILES = {
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/system/bin/qemu-props",
        "/system/bin/qemud",
        "/system/lib/libc_malloc_debug_qemu.so",
        "/sys/qemu_trace",
        "/system/bin/qemu.props",
        "/dev/socket/genyd",
        "/dev/socket/baseband_genyd"
    };
    
    // 危险的系统属性列表
    private static final String[] DANGEROUS_PROPERTIES = {
        "ro.debuggable",
        "ro.secure",
        "service.adb.root",
        "ro.product.model",
        "ro.product.name",
        "ro.product.device",
        "init.svc.qemud",
        "init.svc.goldfish-logcat"
    };
    
    // 模拟器特征属性值列表
    private static final String[] EMULATOR_PROPERTIES = {
        "generic",
        "generic_x86",
        "sdk",
        "sdk_x86",
        "vbox86p",
        "emulator",
        "simulator",
        "goldfish"
    };
    
    // Root特征文件
    private static final String[] ROOT_INDICATORS = {
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/bin/failsafe/su",
        "/system/bin/su",
        "/system/etc/init.d/99SuperSUDaemon",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/system/sd/xbin/su"
    };
    
    // Root应用包名
    private static final String[] ROOT_PACKAGES = {
        "eu.chainfire.supersu",
        "com.noshufou.android.su",
        "com.koushikdutta.superuser",
        "com.zachspong.temprootremovejb",
        "com.ramdroid.appquarantine",
        "com.topjohnwu.magisk"
    };
    
    // 深度安全检查是否已执行
    private static volatile boolean deepCheckPerformed = false;
    
    // 安全检查计数器
    private static volatile int securityCheckCount = 0;
    
    // 随机数生成器
    private static final Random RANDOM = new Random();
    
    // 静态初始化块 - 在类加载时执行安全检查
    static {
        // 执行快速安全检查
        if (!quickSecurityCheck()) {
            securityCompromised.set(true);
        }
        securityCheckPerformed.set(true);
        
        // 随机混淆
        if (RANDOM.nextDouble() < 0.3) {
            CodeObfuscator.insertJunkCode(10);
        }
    }
    
    // 私有构造函数，防止实例化
    private SecurityGuardian() {
        throw new UnsupportedOperationException("实用工具类不允许实例化");
    }
    
    /**
     * 快速安全检查
     * 随机选择一种检查方式执行基本安全验证
     */
    public static boolean quickSecurityCheck() {
        // 增加检查计数
        securityCheckCount++;
        
        // 如果已经确认安全性受损，则直接返回失败
        if (securityCompromised.get()) {
            return false;
        }
        
        // 定期执行一些混淆操作
        if (securityCheckCount % 5 == 0) {
            CodeObfuscator.insertJunkCode(5);
        }
        
        try {
            // 随机选择检查类型
            int checkType = RANDOM.nextInt(5);
            
            switch (checkType) {
                case 0:
                    // 检查调试器
                    return !isDebuggerConnected();
                case 1:
                    // 检查可调试性
                    return !checkDebuggable();
                case 2:
                    // 检查调用栈
                    return isCallStackSafe();
                case 3:
                    // 检查调试类是否加载
                    return !areDebugClassesLoaded();
                default:
                    // 检查模拟器
                    return !checkEmulatorFiles();
            }
        } catch (Exception e) {
            // 发生异常可能是安全检查本身被干扰
            securityCompromised.set(true);
            return false;
        }
    }
    
    /**
     * 检查是否是安全环境
     * 执行多项安全检查验证
     */
    public static boolean isSecureEnvironment() {
        // 如果已经确认安全性受损，则直接返回失败
        if (securityCompromised.get()) {
            return false;
        }
        
        // 引入一些随机性的混淆
        if (RANDOM.nextDouble() < 0.3) {
            CodeObfuscator.obfuscateExecutionFlow();
        }
        
        try {
            // 默认假设是安全的
            boolean secure = true;
            
            // 检查是否有调试器连接
            secure = secure && !isDebuggerConnected();
            
            // 检查危险的系统属性
            secure = secure && !checkDangerousProperties();
            
            // 检查Root特征
            secure = secure && !isDeviceRooted();
            
            // 检查模拟器特征
            secure = secure && !isRunningOnEmulator();
            
            return secure;
        } catch (Exception e) {
            securityCompromised.set(true);
            return false;
        }
    }
    
    /**
     * 深度安全检查
     * 执行更全面和资源密集的安全检查，适合关键操作前调用
     */
    public static boolean performDeepSecurityCheck() {
        // 如果已经执行过深度检查并且确认安全性受损，则直接返回失败
        if (deepCheckPerformed && securityCompromised.get()) {
            return false;
        }
        
        try {
            // 标记深度检查已执行
            deepCheckPerformed = true;
            
            // 检查基本安全环境
            if (!isSecureEnvironment()) {
                securityCompromised.set(true);
                return false;
            }
            
            // 检查应用完整性
            if (!checkAppIntegrity()) {
                securityCompromised.set(true);
                return false;
            }
            
            // 检查是否有Hook框架
            if (isFrameworkHooked()) {
                securityCompromised.set(true);
                return false;
            }
            
            // 检查VPN和代理
            if (isNetworkCompromised()) {
                // 网络被篡改只是警告，不一定标记为不安全
                // 这里可以记录日志或采取额外措施
            }
            
            return true;
        } catch (Exception e) {
            securityCompromised.set(true);
            return false;
        }
    }
    
    /**
     * 检查调试器是否连接
     */
    private static boolean isDebuggerConnected() {
        return Debug.isDebuggerConnected();
    }
    
    /**
     * 检查应用是否可调试
     */
    private static boolean checkDebuggable() {
        try {
            // 检查系统属性
            String debuggable = getSystemProperty("ro.debuggable");
            if (debuggable != null && debuggable.equals("1")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查调用栈是否安全
     */
    private static boolean isCallStackSafe() {
        try {
            // 获取当前调用栈
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            
            // 检查调用栈中是否有可疑的类
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                for (String debugPackage : DEBUG_PACKAGES) {
                    if (className.startsWith(debugPackage)) {
                        return false;
                    }
                }
                
                // 检查是否有动态代理或反射调用
                if (className.contains("$Proxy") || 
                    className.contains("java.lang.reflect.") || 
                    className.contains("dalvik.system.")) {
                    // 找到可疑的类，进一步验证方法名
                    String methodName = element.getMethodName();
                    if (methodName.contains("invoke") || 
                        methodName.contains("newInstance") || 
                        methodName.contains("getMethod")) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            // 出现异常可能是由于安全检查代码本身被Hook
            return false;
        }
    }
    
    /**
     * 检查调试相关的类是否已加载
     */
    private static boolean areDebugClassesLoaded() {
        try {
            for (String debugPackage : DEBUG_PACKAGES) {
                try {
                    Class.forName(debugPackage);
                    return true;
                } catch (ClassNotFoundException e) {
                    // 类未找到，继续检查下一个
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查模拟器特征文件
     */
    private static boolean checkEmulatorFiles() {
        for (String path : EMULATOR_FILES) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查危险的系统属性
     */
    private static boolean checkDangerousProperties() {
        try {
            // 检查调试属性
            if ("1".equals(getSystemProperty("ro.debuggable"))) {
                return true;
            }
            
            // 检查安全属性
            if ("0".equals(getSystemProperty("ro.secure"))) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查设备是否已Root
     */
    private static boolean isDeviceRooted() {
        // 检查特征文件
        for (String path : ROOT_INDICATORS) {
            if (new File(path).exists()) {
                return true;
            }
        }
        
        // 尝试执行su命令
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
            return true;
        } catch (Exception e) {
            // 无法执行su命令，可能没有Root
        }
        
        return false;
    }
    
    /**
     * 检查是否在模拟器上运行
     */
    private static boolean isRunningOnEmulator() {
        try {
            // 检查属性
            String model = getSystemProperty("ro.product.model");
            String manufacturer = getSystemProperty("ro.product.manufacturer");
            String brand = getSystemProperty("ro.product.brand");
            String device = getSystemProperty("ro.product.device");
            
            // 检查特征属性值
            for (String property : EMULATOR_PROPERTIES) {
                if ((model != null && model.contains(property)) ||
                    (manufacturer != null && manufacturer.contains(property)) ||
                    (brand != null && brand.contains(property)) ||
                    (device != null && device.contains(property))) {
                    return true;
                }
            }
            
            // 检查电话号码
            if ("15555215554".equals(getSystemProperty("ril.test.mode")) ||
                "15555215554".equals(getSystemProperty("gsm.sim.subscriber.number"))) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查应用完整性
     */
    private static boolean checkAppIntegrity() {
        // 实现应用完整性检查逻辑
        // 这里可以检查应用签名、APK哈希等
        return true; // 假设验证通过
    }
    
    /**
     * 检查是否被Hook框架攻击
     */
    private static boolean isFrameworkHooked() {
        try {
            // 检查已加载的类
            Set<String> frameworkPrefixes = new HashSet<>(Arrays.asList(
                "de.robv.android.xposed", 
                "com.saurik.substrate",
                "com.github.moduth.blockcanary"
            ));
            
            // 尝试获取已加载的类数组（这是一种启发式方法）
            try {
                Field field = ClassLoader.class.getDeclaredField("classes");
                field.setAccessible(true);
                Object value = field.get(ClassLoader.getSystemClassLoader());
                if (value instanceof Class<?>[]) {
                    Class<?>[] classes = (Class<?>[]) value;
                    for (Class<?> clazz : classes) {
                        if (clazz != null) {
                            String name = clazz.getName();
                            for (String prefix : frameworkPrefixes) {
                                if (name.startsWith(prefix)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略反射异常
            }
            
            // 检查Xposed的环境变量
            if (System.getenv("CLASSPATH") != null && 
                System.getenv("CLASSPATH").contains("Xposed")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查网络环境是否被劫持
     */
    private static boolean isNetworkCompromised() {
        // 实现网络环境检查逻辑
        // 可以检查代理设置、VPN状态等
        return false; // 假设网络环境安全
    }
    
    /**
     * 获取系统属性
     */
    private static String getSystemProperty(String name) {
        try {
            // 通过Runtime执行getprop命令
            Process process = Runtime.getRuntime().exec("getprop " + name);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.destroy();
            return line;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 重置安全状态（仅用于测试）
     */
    public static void resetSecurityState() {
        securityCompromised.set(false);
        securityCheckPerformed.set(false);
        deepCheckPerformed = false;
    }
}