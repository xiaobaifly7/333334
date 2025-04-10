package com.hanbing.injectedpopup.entry;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hanbing.injectedpopup.config.ConfigModule;
import com.hanbing.injectedpopup.dialog.DialogModule;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 注入代码的入口点
 * 负责协调配置获取、解密、解析和弹窗显示
 */
public class InjectionEntryPoint {

    private static final String TAG = "InjectionEntry";
    // 使用单线程池处理后台任务，避免并发问题
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * 显示弹窗的公共静态方法 (注入后调用此方法)
     * @param activity 当前活动的 Activity Context
     */
    public static void showPopup(final Activity activity) {
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "Activity is null or finishing, cannot show popup.");
            return;
        }
        Log.d(TAG, "showPopup called.");

        // 在后台线程获取和处理配置
        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Background task started: Fetching config...");
                ConfigModule configModule = ConfigModule.getInstance(activity.getApplicationContext());
                // 1. 获取加密的配置字符串 (包含多线路选择和延迟优化逻辑)
                byte[] encryptedConfigData = configModule.fetchEncryptedConfig();

                if (encryptedConfigData == null || encryptedConfigData.length == 0) {
                    Log.e(TAG, "Failed to fetch encrypted config.");
                    // 可选：尝试从缓存加载旧配置或显示错误提示
                    loadConfigFromCacheAndShow(activity, configModule);
                    return;
                }
                Log.d(TAG, "Encrypted config fetched successfully (" + encryptedConfigData.length + " bytes).");

                // 2. 获取解密密钥 (包含KDF逻辑) - **需要Native实现**
                byte[] decryptionKey = configModule.getDecryptionKey();
                if (decryptionKey == null || decryptionKey.length == 0) {
                    Log.e(TAG, "Failed to get decryption key.");
                    loadConfigFromCacheAndShow(activity, configModule); // 尝试缓存
                    return;
                }
                Log.d(TAG, "Decryption key obtained.");

                // 3. 解密配置 - **需要Native实现**
                String jsonConfig = configModule.decryptConfig(encryptedConfigData, decryptionKey);
                 // 清理密钥
                java.util.Arrays.fill(decryptionKey, (byte) 0);

                if (jsonConfig == null || jsonConfig.isEmpty()) {
                    Log.e(TAG, "Failed to decrypt config.");
                    loadConfigFromCacheAndShow(activity, configModule); // 尝试缓存
                    return;
                }
                Log.d(TAG, "Config decrypted successfully.");

                // 4. 解析 JSON 配置
                JSONObject configObject = configModule.parseConfig(jsonConfig);
                if (configObject == null) {
                    Log.e(TAG, "Failed to parse JSON config.");
                    loadConfigFromCacheAndShow(activity, configModule); // 尝试缓存
                    return;
                }
                Log.d(TAG, "JSON config parsed successfully.");

                // 5. 检查是否需要显示 (版本忽略逻辑)
                if (!configModule.shouldShowDialog(configObject)) {
                    Log.i(TAG, "Dialog should not be shown based on config version or rules.");
                    return;
                }

                // 6. 在主线程显示对话框
                mainThreadHandler.post(() -> {
                    Log.d(TAG, "Posting dialog display to main thread.");
                    DialogModule.showDialog(activity, configObject);
                });

                // 7. 成功获取并处理后，更新缓存
                configModule.updateCache(jsonConfig);

            } catch (Exception e) {
                Log.e(TAG, "Error during popup process", e);
                // 发生任何异常时，尝试从缓存加载
                loadConfigFromCacheAndShow(activity, ConfigModule.getInstance(activity.getApplicationContext()));
            }
        });
    }

    /**
     * 尝试从缓存加载配置并显示
     */
    private static void loadConfigFromCacheAndShow(final Activity activity, final ConfigModule configModule) {
        Log.w(TAG, "Attempting to load config from cache...");
        backgroundExecutor.execute(() -> {
            try {
                String cachedJsonConfig = configModule.getCachedConfig();
                if (cachedJsonConfig != null && !cachedJsonConfig.isEmpty()) {
                    JSONObject configObject = configModule.parseConfig(cachedJsonConfig);
                    if (configObject != null && configModule.shouldShowDialog(configObject)) {
                        mainThreadHandler.post(() -> {
                            Log.i(TAG, "Showing dialog from cached config.");
                            DialogModule.showDialog(activity, configObject);
                        });
                    } else {
                        Log.i(TAG, "Cached config not valid or should not be shown.");
                    }
                } else {
                    Log.i(TAG, "No valid cached config found.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading config from cache", e);
            }
        });
    }
}
