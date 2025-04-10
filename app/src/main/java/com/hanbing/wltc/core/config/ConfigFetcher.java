package com.hanbing.wltc.core.config;

import android.util.Log;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 配置获取器
 * 负责从远程服务器获取和更新配置信息
 */
public final class ConfigFetcher {
    
    private static final String TAG = "ConfigFetcher";
    
    // 单例实例
    private static volatile ConfigFetcher INSTANCE;
    
    // 配置更新间隔 (6小时)
    private static final long CONFIG_UPDATE_INTERVAL = 6 * 60 * 60 * 1000;
    
    // 上次更新时间
    private volatile long lastUpdateTime = 0;
    
    // 是否正在更新
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // 更新回调接口
    private ConfigUpdateCallback updateCallback;
    
    // 默认配置
    private String defaultConfig = "{}";
    
    /**
     * 配置更新回调接口
     */
    public interface ConfigUpdateCallback {
        void onConfigUpdated(String config);
        void onConfigUpdateFailed(Exception e);
    }
    
    /**
     * 私有构造函数
     */
    private ConfigFetcher() {
        // 初始化配置管理器
        SecureConfigManager.initialize();
    }
    
    /**
     * 获取实例
     */
    public static ConfigFetcher getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigFetcher.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConfigFetcher();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 设置更新回调
     */
    public void setUpdateCallback(ConfigUpdateCallback callback) {
        this.updateCallback = callback;
    }
    
    /**
     * 设置默认配置
     */
    public void setDefaultConfig(String defaultConfig) {
        if (defaultConfig != null && !defaultConfig.isEmpty()) {
            this.defaultConfig = defaultConfig;
        }
    }
    
    /**
     * 获取配置
     * 尝试从缓存获取，如果缓存不存在则从网络获取
     */
    public String getConfig() {
        // 尝试从安全存储中获取缓存配置
        String cachedConfig = SecureConfigManager.getSecureConfig();
        if (cachedConfig != null && !cachedConfig.isEmpty()) {
            // 检查是否需要在后台更新配置
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > CONFIG_UPDATE_INTERVAL) {
                updateConfigAsync();
            }
            return cachedConfig;
        }
        
        // 如果没有缓存配置，尝试同步获取
        try {
            String config = fetchConfig();
            if (config != null && !config.isEmpty()) {
                return config;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取配置失败", e);
        }
        
        // 如果同步获取失败，异步更新并返回默认配置
        updateConfigAsync();
        return defaultConfig;
    }
    
    /**
     * 强制更新
     */
    public void forceUpdate() {
        updateConfigAsync();
    }
    
    /**
     * 异步更新配置
     */
    private void updateConfigAsync() {
        if (isUpdating.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    String config = fetchConfig();
                    if (config != null && !config.isEmpty()) {
                        lastUpdateTime = System.currentTimeMillis();
                        
                        // 回调通知
                        if (updateCallback != null) {
                            updateCallback.onConfigUpdated(config);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新配置失败", e);
                    
                    // 回调通知
                    if (updateCallback != null) {
                        updateCallback.onConfigUpdateFailed(e);
                    }
                } finally {
                    isUpdating.set(false);
                }
            }).start();
        }
    }
    
    /**
     * 获取远程配置
     */
    private String fetchConfig() throws IOException, SecurityException {
        // 安全检查
        if (!SecurityGuardian.quickSecurityCheck()) {
            throw new SecurityException("安全检查失败");
        }
        
        // 1. 获取安全URL基址
        String baseUrl = UltraSecurityManager.getInstance().getSecureUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IOException("无法获取安全URL");
        }
        
        // 2. 获取多个配置文件路径（用于容灾）
        String[] configPaths = SecureConfigManager.getConfigFilePaths();
        if (configPaths == null || configPaths.length == 0) {
            throw new IOException("配置路径列表为空");
        }
        
        // 随机打乱顺序，避免总是请求同一个
        shuffleArray(configPaths);
        
        // 3. 尝试获取配置内容
        Exception lastException = null;
        for (String configPath : configPaths) {
            try {
                // 构建URL
                String configUrl = baseUrl + configPath + "?t=" + System.currentTimeMillis();
                Log.d(TAG, "尝试请求配置: " + configUrl);
                
                // 安全连接
                String encryptedConfig = NetworkSecurityEnhancer.secureConnect(configUrl);
                if (encryptedConfig != null && !encryptedConfig.isEmpty()) {
                    // 解密配置内容
                    String decryptedConfig = SecureConfigManager.decryptConfig(encryptedConfig);
                    if (decryptedConfig != null && !decryptedConfig.isEmpty()) {
                        Log.d(TAG, "配置获取成功");
                        return decryptedConfig;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "获取配置失败: " + configPath, e);
                lastException = e;
                // 继续尝试下一个
            }
        }
        
        // 所有尝试都失败
        if (lastException != null) {
            throw new IOException("所有配置源获取失败", lastException);
        } else {
            throw new IOException("所有配置内容无效");
        }
    }
    
    /**
     * 随机打乱数组顺序
     */
    private void shuffleArray(String[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            // 交换元素
            String temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}