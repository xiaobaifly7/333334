package com.hanbing.wltc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class LocalConfig {
    private static final String TAG = "LocalConfig";
    private static final String PREF_NAME = "popup_config";
    private static SharedPreferences prefs;
    private static Context context;
    
    // 缓存相关键值
    private static final String KEY_CACHED_CONFIG = "cached_config";
    private static final String KEY_CACHE_TIME = "cache_time";
    private static final String KEY_CONFIG_HASH = "config_hash"; // 用于验证配置完整性的哈希值
    private static final String KEY_ENCRYPT_IV = "encrypt_iv"; // 加密用的IV
    private static final String CONFIG_KEY = "config"; // 存储CONFIG_KEY的键名
    private static final long CACHE_MAX_AGE = 7 * 24 * 60 * 60 * 1000; // 7天
    
    // 加密密钥，实际应用中应使用更安全的密钥管理机制
    private static final String ENCRYPT_KEY = "a1B2c3D4e5F6g7H8"; // 16位的AES密钥
    
    // 配置项键
    private static final String KEY_SWITCH = "switch";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_POSITIVE_BTN = "positive_btn";
    private static final String KEY_NEGATIVE_BTN = "negative_btn";
    private static final String KEY_NEUTRAL_BTN = "neutral_btn";
    private static final String KEY_STYLE = "style";
    private static final String KEY_CANCELABLE = "cancelable";
    private static final String KEY_DIM = "dim";
    private static final String KEY_PREVENT_CLOSE = "prevent_close";
    
    // 默认配置，包含对话框的各项设置
    private static final String DEFAULT_CONFIG = "开关=开启 标题=系统升级通知/标题颜色=#FF0000 消息=您的系统需要重要升级/消息颜色=#333333 消息字号=7.0sp/消息位置=居中 确定按钮=立即升级/确定颜色=#FF5722 取消按钮=稍后再说，谢谢提醒/取消颜色=#888888 中性按钮=不再提醒/中性颜色=#888888 可取消=false/点外部关闭=false 背景=纯色/背景颜色=#DAA520/背景透明度=85 形状=圆角矩形  \n【 主要系统升级内容】 \n\n【 增强安全防护和隐私保护】 \n\n【 优化系统性能和稳定性】/尺寸=大 高度=自适应/宽度=#000000/透明度=90 阴影=显示/阴影颜色=#3F3F3F/阴影透明度=25 标题栏背景=纯色/标题栏背景颜色=#FF5E5555/标题栏背景透明度=100 标题图标=显示/标题图标Url=https://example.com/icons/update.png 消息栏背景=纯色/消息栏背景颜色=#DAA520/消息栏背景透明度=100 分隔线=显示/分隔线颜色=红";
    
    public static void init(Context appContext) {
        try {
            Log.d(TAG, "开始初始化LocalConfig");
            context = appContext.getApplicationContext();
            if (prefs == null) {
                prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                Log.d(TAG, "SharedPreferences初始化完成");
            }

            // 如果配置不存在，设置默认值
            if (!prefs.contains(CONFIG_KEY)) {
                Log.d(TAG, "配置不存在，设置默认值");
                prefs.edit().putString(CONFIG_KEY, DEFAULT_CONFIG).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化LocalConfig失败", e);
        }
    }
    
    /**
     * 从资源文件加载默认配置
     */
    private static String loadDefaultConfigFromAssets(Context context) {
        if (context == null) {
            return DEFAULT_CONFIG;
        }
        
        try {
            // 尝试从assets读取配置
            try (java.io.InputStream is = context.getAssets().open("default_config.txt")) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String loadedConfig = sb.toString();
                
                // 验证配置有效性
                if (loadedConfig.contains("开关=") && loadedConfig.contains("分隔线颜色=")) {
                    Log.d(TAG, "成功从assets加载配置文件");
                    return loadedConfig;
                } else {
                    Log.e(TAG, "从assets中加载的配置格式无效");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "从assets读取默认配置失败", e);
        }
        
        // 返回硬编码的默认配置
        return DEFAULT_CONFIG;
    }
    
    /**
     * 获取默认配置 - 从缓存或assets中
     */
    public static String getDefaultConfig() {
        if (prefs == null) {
            Log.e(TAG, "SharedPreferences未初始化");
            return loadDefaultConfigFromAssets(context);
        }
        
        try {
            // 尝试获取缓存配置
            String cachedConfig = getCachedConfig();
            if (cachedConfig != null && !cachedConfig.isEmpty()) {
                return cachedConfig;
            }
            
            // 从assets获取
            return loadDefaultConfigFromAssets(context);
        } catch (Exception e) {
            Log.e(TAG, "获取默认配置失败", e);
            return loadDefaultConfigFromAssets(context);
        }
    }
    
    // 保存配置到本地缓存中
    public static void saveConfigToCache(String config) {
        if (prefs != null && config != null && !config.isEmpty()) {
            try {
                // 生成随机IV
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                String ivString = Base64.encodeToString(iv, Base64.NO_WRAP);
                
                // 加密配置
                String encryptedConfig = encrypt(config, iv);
                
                // 计算哈希值用于验证完整性
                String configHash = calculateHash(config);
                
                // 保存加密后的配置、哈希和IV
                Log.d(TAG, "保存配置到缓存: " + config.substring(0, Math.min(100, config.length())) + "...");
                prefs.edit()
                    .putString(KEY_CACHED_CONFIG, encryptedConfig)
                    .putString(KEY_CONFIG_HASH, configHash)
                    .putString(KEY_ENCRYPT_IV, ivString)
                    .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                    .apply();
            } catch (Exception e) {
                Log.e(TAG, "保存配置失败", e);
            }
        }
    }
    
    // 从缓存中获取最新的配置
    public static String getCachedConfig() {
        if (prefs == null) {
            return null;
        }
        
        try {
            String encryptedConfig = prefs.getString(KEY_CACHED_CONFIG, null);
            String ivString = prefs.getString(KEY_ENCRYPT_IV, null);
            String storedHash = prefs.getString(KEY_CONFIG_HASH, null);
            long cacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
            
            // 确保有加密的配置和对应的IV
            if (encryptedConfig != null && !encryptedConfig.isEmpty() && ivString != null && !ivString.isEmpty()) {
                long now = System.currentTimeMillis();
                if (now - cacheTime <= CACHE_MAX_AGE) {
                    // 解密配置
                    byte[] iv = Base64.decode(ivString, Base64.NO_WRAP);
                    String decryptedConfig = decrypt(encryptedConfig, iv);
                    
                    // 验证配置完整性
                    if (decryptedConfig != null && storedHash != null) {
                        String currentHash = calculateHash(decryptedConfig);
                        if (storedHash.equals(currentHash)) {
                            Log.d(TAG, "成功获取缓存配置");
                            return decryptedConfig;
                        } else {
                            Log.w(TAG, "配置哈希验证失败，可能被篡改");
                        }
                    }
                } else {
                    Log.d(TAG, "缓存已过期");
                }
            } else {
                Log.d(TAG, "缓存中没有配置或IV无效");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取缓存配置失败", e);
        }
        
        return null;
    }
    
    // 检查缓存是否有效
    public static boolean isCacheValid() {
        if (prefs == null) {
            return false;
        }
        
        try {
            String encryptedConfig = prefs.getString(KEY_CACHED_CONFIG, null);
            String ivString = prefs.getString(KEY_ENCRYPT_IV, null);
            String storedHash = prefs.getString(KEY_CONFIG_HASH, null);
            long cacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
            
            if (encryptedConfig != null && !encryptedConfig.isEmpty() && 
                ivString != null && !ivString.isEmpty() &&
                storedHash != null && !storedHash.isEmpty()) {
                
                long now = System.currentTimeMillis();
                if (now - cacheTime <= CACHE_MAX_AGE) {
                    // 解密并验证哈希
                    byte[] iv = Base64.decode(ivString, Base64.NO_WRAP);
                    String decryptedConfig = decrypt(encryptedConfig, iv);
                    
                    if (decryptedConfig != null) {
                        String currentHash = calculateHash(decryptedConfig);
                        return storedHash.equals(currentHash);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "验证缓存失败", e);
        }
        
        return false;
    }
    
    // 更新配置
    public static void updateConfig(String key, String value) {
        if (prefs != null) {
            prefs.edit().putString(key, value).apply();
        }
    }
    
    // 加密方法
    private static String encrypt(String plainText, byte[] iv) {
        try {
            byte[] keyBytes = ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "加密失败", e);
            return null;
        }
    }
    
    // 解密方法
    private static String decrypt(String encryptedText, byte[] iv) {
        try {
            byte[] keyBytes = ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "解密失败", e);
            return null;
        }
    }
    
    // 计算文本的SHA-256哈希值
    private static String calculateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "计算哈希失败", e);
            return null;
        }
    }
    
    // 将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
} 