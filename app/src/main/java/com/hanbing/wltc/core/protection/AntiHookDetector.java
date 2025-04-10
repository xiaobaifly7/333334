package com.hanbing.wltc.core.protection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hook框架检测器
 * 用于检测设备上是否存在Hook框架
 */
public final class AntiHookDetector {
    
    // 是否检测到Hook框架（缓存结果）
    private static final AtomicBoolean HOOK_DETECTED = new AtomicBoolean(false);
    
    // 常见的Hook框架包名
    private static final String[] HOOK_PACKAGES = {
        "de.robv.android.xposed",
        "com.saurik.substrate",
        "com.android.tools.fd.runtime",
        "org.jf.dexlib",
        "dalvik.system.VMDebug",
        "com.saurik.substrate",
        "de.larma.arthook",
        "de.robv.android.xposed.XposedBridge",
        "com.taobao.android.dexposed",
        "me.weishu.epic",
        "me.weishu.freereflection",
        "com.swift.sandhook",
        "com.elderdrivers.riru"
    };
    
    // 可疑的库文件
    private static final String[] SUSPICIOUS_LIBRARIES = {
        "libsubstrate.so",
        "libxposed_art.so",
        "libfrida-agent.so",
        "libygfrida.so",
        "libdexposed.so",
        "libwhale.so",
        "libsandhook.so",
        "libsandhook-native.so",
        "libnativehook.so",
        "libmemtrack_real.so",
        "libmemhook.so"
    };
    
    // 可疑的文件路径
    private static final String[] SUSPICIOUS_FILES = {
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/system/xposed.prop",
        "/system/framework/XposedBridge.jar",
        "/data/local/tmp/frida-server",
        "/data/local/tmp/re.frida.server",
        "/data/local/tmp/frida-gadget.so",
        "/system/bin/app_process.orig",
        "/system/bin/app_process32_xposed",
        "/system/bin/app_process64_xposed",
        "/system/etc/installed_su_daemon",
        "/sbin/su",
        "/system/xbin/su",
        "/system/bin/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/data/local/su"
    };
    
    // 可疑的环境变量
    private static final String[] SUSPICIOUS_ENV_VARS = {
        "CLASSPATH=/data/app/de.robv.android.xposed.installer",
        "PATH=/system/xposed/bin",
        "LD_LIBRARY_PATH=/system/lib/xposed",
        "XPOSED_LOAD_MODULES=1"
    };
    
    // 常见的Hook框架类
    private static final String[] HOOK_CLASSES = {
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers",
        "de.robv.android.xposed.DexposedBridge",
        "de.robv.android.xposed.IXposedHookLoadPackage",
        "com.saurik.substrate.MS",
        "com.swift.sandhook.SandHook",
        "me.weishu.epic.art.Epic",
        "me.weishu.epic.art.EpicNative",
        "me.weishu.reflection.Reflection"
    };
    
    // 静态初始化
    static {
        performInitialCheck();
    }
    
    // 私有构造函数
    private AntiHookDetector() {
        throw new UnsupportedOperationException("Hook框架检测器不允许实例化");
    }
    
    /**
     * 执行初始检查
     */
    private static void performInitialCheck() {
        try {
            // 执行基本检查项目
            boolean hookDetected = isHookFrameworkPresent() || 
                                  checkClassloaders() || 
                                  checkNativeLibraries() || 
                                  checkStackTrace() || 
                                  checkReflection();
            
            // 设置结果到缓存中
            HOOK_DETECTED.set(hookDetected);
            
        } catch (Throwable ignored) {
            // 检查过程中出现异常，默认设为true
            // 因为正常环境下不太可能出现异常，异常可能是Hook框架干扰造成的
            HOOK_DETECTED.set(true);
        }
    }
    
    /**
     * 检查是否存在Hook框架
     */
    public static boolean isHookDetected() {
        // 如果之前已检测到
        if (HOOK_DETECTED.get()) {
            return true;
        }
        
        try {
            // 执行快速检查
            boolean result = isHookFrameworkPresent() || checkSystemProperties();
            
            // 如果快速检查未发现问题，执行更深入的检查
            if (!result) {
                result = checkClassloaders() || 
                        checkNativeLibraries() || 
                        checkStackTrace() || 
                        checkReflection() ||
                        checkMethodHooks();
            }
            
            // 更新缓存
            if (result) {
                HOOK_DETECTED.set(true);
            }
            
            return result;
        } catch (Throwable t) {
            // 发生异常可能是Hook框架干扰
            HOOK_DETECTED.set(true);
            return true;
        }
    }
    
    /**
     * 检查是否存在Hook框架
     */
    private static boolean isHookFrameworkPresent() {
        try {
            // 1. 检查Xposed
            try {
                ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge");
                return true;
            } catch (ClassNotFoundException ignored) {
                // 没有发现Xposed
            }
            
            // 2. 检查Frida
            if (checkFrida()) {
                return true;
            }
            
            // 3. 检查Substrate
            try {
                ClassLoader.getSystemClassLoader().loadClass("com.saurik.substrate.MS");
                return true;
            } catch (ClassNotFoundException ignored) {
                // 没有发现Substrate
            }
            
            // 4. 检查可疑文件
            for (String path : SUSPICIOUS_FILES) {
                if (new File(path).exists()) {
                    return true;
                }
            }
            
            // 5. 检查/proc/maps中是否有可疑模块
            if (checkProcMaps()) {
                return true;
            }
            
            return false;
        } catch (Throwable t) {
            // 出现异常，可能是某些检测方法被Hook
            return true;
        }
    }
};