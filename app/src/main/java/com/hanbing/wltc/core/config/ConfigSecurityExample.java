package com.hanbing.wltc.core.config;

import android.content.Context;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 配置安全示例
 * 提供安全的配置管理和特性开关功能
 */
public class ConfigSecurityExample {
    
    private static final String TAG = "ConfigSecurityExample";
    
    /**
     * 初始化安全配置管理系统
     */
    public static void initialize(Context context) {
        try {
            // 启用安全防护功能
            UltraSecurityManager.getInstance().enableMemoryProtection();
            UltraSecurityManager.getInstance().enableNetworkSecurity();
            
            // 初始化配置管理器
            SecureConfigManager.initialize();
            
            // 设置默认配置，用于网络请求失败时的备选方案
            ConfigFetcher fetcher = ConfigFetcher.getInstance();
            fetcher.setDefaultConfig("{\"version\":\"1.0\",\"features\":{\"feature1\":false,\"feature2\":false}}");
            
            // 设置更新回调
            fetcher.setUpdateCallback(new ConfigFetcher.ConfigUpdateCallback() {
                @Override
                public void onConfigUpdated(String config) {
                    Log.d(TAG, "配置更新成功");
                    // 这里可以添加配置更新后的其他操作
                }
                
                @Override
                public void onConfigUpdateFailed(Exception e) {
                    Log.e(TAG, "配置更新失败", e);
                    // 这里可以添加失败处理逻辑
                }
            });
            
            // 启动配置更新
            new Thread(() -> {
                try {
                    String config = fetcher.getConfig();
                    Log.d(TAG, "获取配置成功");
                } catch (Exception e) {
                    Log.e(TAG, "获取配置失败", e);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "初始化安全配置管理系统失败", e);
        }
    }
    
    /**
     * 检查特定功能是否启用
     */
    public static boolean isFeatureEnabled(String featureName, boolean defaultValue) {
        try {
            String config = ConfigFetcher.getInstance().getConfig();
            JSONObject json = new JSONObject(config);
            
            if (json.has("features")) {
                JSONObject features = json.getJSONObject("features");
                if (features.has(featureName)) {
                    return features.getBoolean(featureName);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析配置失败", e);
        } catch (Exception e) {
            Log.e(TAG, "获取功能状态异常", e);
        }
        
        return defaultValue;
    }
    
    /**
     * 获取配置参数
     */
    public static String getConfigParam(String paramPath, String defaultValue) {
        try {
            String config = ConfigFetcher.getInstance().getConfig();
            JSONObject json = new JSONObject(config);
            
            String[] pathParts = paramPath.split("\\.");
            JSONObject currentObj = json;
            
            // 遍历路径
            for (int i = 0; i < pathParts.length - 1; i++) {
                if (currentObj.has(pathParts[i])) {
                    Object obj = currentObj.get(pathParts[i]);
                    if (obj instanceof JSONObject) {
                        currentObj = (JSONObject) obj;
                    } else {
                        return defaultValue;
                    }
                } else {
                    return defaultValue;
                }
            }
            
            // 获取最终值
            String lastKey = pathParts[pathParts.length - 1];
            if (currentObj.has(lastKey)) {
                return currentObj.get(lastKey).toString();
            }
        } catch (JSONException e) {
            Log.e(TAG, "解析配置失败", e);
        } catch (Exception e) {
            Log.e(TAG, "获取配置参数失败", e);
        }
        
        return defaultValue;
    }
    
    /**
     * 获取完整配置
     */
    public static String getFullConfig() {
        try {
            return ConfigFetcher.getInstance().getConfig();
        } catch (Exception e) {
            Log.e(TAG, "获取完整配置失败", e);
            return "{}";
        }
    }
    
    /**
     * 强制更新配置
     */
    public static void forceUpdateConfig() {
        try {
            ConfigFetcher.getInstance().forceUpdate();
        } catch (Exception e) {
            Log.e(TAG, "强制更新配置失败", e);
        }
    }
    
    /**
     * 获取配置预处理命令示例
     */
    public static String getPreprocessCommand(String configFile, String deviceGroup) {
        return "java -jar config-encryption-tool.jar " + 
               configFile + " " + 
               deviceGroup + " " + 
               "./configs";
    }
}