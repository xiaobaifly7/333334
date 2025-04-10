package com.hanbing.wltc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * 应用 Activity 生命周期回调处理类
 * 用于监听应用中所有 Activity 的生命周期事件，并在特定时机执行初始化或资源释放操作。
 */
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "LifecycleHandler";
    private static int activityCount = 0; // 记录活动的 Activity 数量
    private static boolean isAppInBackground = false; // 标记应用是否处于后台

    /**
     * 初始化并注册生命周期回调
     * @param application 应用实例
     */
    public static void init(Application application) {
        application.registerActivityLifecycleCallbacks(new LifecycleHandler());
        Log.d(TAG, "生命周期回调已注册");
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // 第一个 Activity 创建时，初始化 MultiLineManager
        if (activityCount == 0) {
            MultiLineManager.init(activity.getApplicationContext());
            Log.d(TAG, "MultiLineManager 已初始化");
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityCount++;
        if (activityCount == 1 && isAppInBackground) {
            // 应用从后台回到前台
            isAppInBackground = false;
            Log.d(TAG, "应用回到前台");
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Activity 恢复可见
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Activity 暂停
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            // 应用进入后台 (没有活动的 Activity)
            isAppInBackground = true;
            Log.d(TAG, "应用进入后台");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Activity 状态保存
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // 最后一个 Activity 销毁时或当前 Activity 正在结束时，释放资源
        if (activityCount == 0 || activity.isFinishing()) {
            try {
                han.releaseResources(); // 调用 han 类释放资源
                Log.d(TAG, "相关资源已释放");
            } catch (Exception e) {
                Log.e(TAG, "释放资源时出错", e);
            }
        }
    }
}
