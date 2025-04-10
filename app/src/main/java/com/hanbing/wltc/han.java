package com.hanbing.wltc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;

/* loaded from: C:\Users\Administrator\Downloads\反编译\classes4.dex */
public class han {
    private static final String TAG = "han";
    private static boolean lifecycleInitialized = false;

    public static void diaoyon(Activity activity) {
        try {
            if (activity == null) {
                Log.e(TAG, "显示对话框失败: activity参数为null");
                return;
            }

            // 初始化生命周期管理器
            if (!lifecycleInitialized) {
                Application app = activity.getApplication();
                if (app != null) {
                    LifecycleHandler.init(app);
                    lifecycleInitialized = true;
                    Log.d(TAG, "生命周期管理器初始化完成");
                }
            }

            // 初始化本地配置管理器
            Log.d(TAG, "初始化本地配置");
            LocalConfig.init(activity);
            try {
                // 尝试初始化安全配置管理器（如果存在）
                Class<?> secureManagerClass = Class.forName("com.hanbing.wltc.secure.NanoConfigManager");
                Method initMethod = secureManagerClass.getDeclaredMethod("init", Context.class);
                initMethod.invoke(null, activity.getApplicationContext());
                Log.d(TAG, "安全配置管理器初始化完成");
            } catch (ClassNotFoundException e) {
                // 安全配置管理器不存在，跳过初始化
                Log.d(TAG, "安全配置管理器不存在");
            } catch (Exception e) {
                Log.e(TAG, "安全配置管理器初始化失败", e);
            }

            Log.d(TAG, "初始化MultiLineManager");
            MultiLineManager.init(activity);
            Log.d(TAG, "MultiLineManager初始化完成");

            // 获取远程配置信息
            Log.d(TAG, "获取配置信息");
            String config = MultiLineManager.getConfig();
            Log.d(TAG, "配置信息获取结果: " + (config != null ? "配置长度:" + config.length() : "配置为null"));

            // 显示在线对话框
            if (config != null && !config.isEmpty()) {
                Log.d(TAG, "显示对话框");
                m0(activity, "mutil.OnlineDialog", config);
                Log.d(TAG, "对话框显示完成");
            } else {
                Log.e(TAG, "配置为空，无法显示对话框");
            }
        } catch (Exception e) {
            Log.e(TAG, "显示对话框异常", e);
        }
    }

    // 释放资源，在应用退出时调用
    public static void releaseResources() {
        try {
            MultiLineManager.release();
            Log.d(TAG, "资源释放完成");
        } catch (Exception e) {
            Log.e(TAG, "释放资源异常", e);
        }
    }

    /* renamed from: 显示, reason: contains not printable characters */
    public static void m0(Activity activity, String str, String str2) {
        try {
            if (activity == null || str == null || str2 == null) {
                Log.e(TAG, "参数检查失败，activity: " + (activity == null ? "null" : "非null") +
                           ", class: " + (str == null ? "null" : str) +
                           ", config: " + (str2 == null ? "null" : "长度:" + str2.length()));
                return;
            }

            Log.d(TAG, "加载对话框类OnlineDialog");
            Class<?> cls = Class.forName(str);
            if (cls == null) {
                Log.e(TAG, "类加载失败: " + str);
                return;
            }

            Class<?>[] clsArr = new Class[2];
            try {
                clsArr[0] = Class.forName("android.app.Activity");
                try {
                    clsArr[1] = Class.forName("java.lang.String");

                    Log.d(TAG, "调用show方法显示对话框");
                    cls.getMethod("show", clsArr).invoke(cls.newInstance(), activity, str2);
                    Log.d(TAG, "OnlineDialog.show方法调用完成");
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "找不到String类", e);
                    throw new NoClassDefFoundError(e.getMessage());
                }
            } catch (ClassNotFoundException e2) {
                Log.e(TAG, "找不到Activity类", e2);
                throw new NoClassDefFoundError(e2.getMessage());
            }
        } catch (Exception e3) {
            Log.e(TAG, "反射调用异常", e3);
        }
    }
}