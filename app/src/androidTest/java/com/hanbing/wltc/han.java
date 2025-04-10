package com.hanbing.wltc;

import android.app.Activity;

/**
 * 测试用的han类
 * 用于在测试环境中模拟弹窗调用
 */
public class han {
    /**
     * 调用弹窗
     */
    public static void diaoyon(Activity activity) {
        m0(activity, "com.hanbing.wltc.MockOnlineDialog", "https://test.url/config");
    }

    /**
     * 反射调用方法，仅用于测试
     */
    public static void m0(Activity activity, String className, String url) {
        try {
            // 首先尝试使用模拟对话框类
            try {
                Class<?> cls = Class.forName(className);
                cls.getMethod("show", Activity.class, String.class).invoke(null, activity, url);
                return;
            } catch (ClassNotFoundException e) {
                // 如果找不到模拟类，则尝试使用真实类名
                String realClassName = "mutil.OnlineDialog";
                try {
                    Class<?> cls = Class.forName(realClassName);
                    cls.getMethod("show", Activity.class, String.class).invoke(null, activity, url);
                } catch (Exception e2) {
                    // 如果真实类也不可用，直接使用我们的MockOnlineDialog
                    MockOnlineDialog.show(activity, url);
                }
            }
        } catch (Exception e) {
            // 发生任何异常时，使用模拟对话框
            MockOnlineDialog.show(activity, url);
        }
    }
}