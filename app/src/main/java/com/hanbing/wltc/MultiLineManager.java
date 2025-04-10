package com.hanbing.wltc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiLineManager {
    private static final String TAG = "MultiLineManager";
    private static Context context;
    
    // 服务器URL列表 - 按优先级排序的备选URL列表
    private static final String[] SERVER_URLS = {
        "https://raw.gitcode.com/sssddd55/344/raw/main/rjk",
        "https://raw.githubusercontent.com/square/okhttp/master/README.md",
        "https://raw.githubusercontent.com/square/retrofit/master/README.md",
        "https://raw.githubusercontent.com/google/material-design-icons/master/README.md",
        "https://cdn.jsdelivr.net/gh/ReactiveX/RxJava@master/README.md",
        "https://fastly.jsdelivr.net/gh/square/retrofit@master/README.md"
    };
    
    // 备用URL列表，在主要列表都失败时使用
    private static final String[] BACKUP_URLS = {
        "https://raw.gitcode.com/sssddd55/344/raw/main/rjk",
        "https://fastly.jsdelivr.net/gh/square/okhttp@master/README.md",
        "https://gcore.jsdelivr.net/gh/square/picasso@master/README.md",
        "https://raw.githubusercontent.com/square/picasso/master/README.md"
    };
    
    // 线程池配置，用于异步任务
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int KEEP_ALIVE_TIME = 60; // 秒
    private static final ExecutorService executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());
    
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static String lastWorkingUrl = null;
    private static long lastSuccessTime = 0;
    private static final long REFRESH_INTERVAL = 30 * 60 * 1000; // 30分钟的刷新间隔
    private static boolean backupMode = false;
    
    public static void init(Context appContext) {
        context = appContext.getApplicationContext();
        LocalConfig.init(context);
        
        // 预加载配置，提前准备好数据
        preloadConfig();
    }
    
    // 释放资源，关闭线程池等
    public static void release() {
        try {
            if (executor != null && !executor.isShutdown()) {
                // 尝试优雅地关闭线程池
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    // 强制关闭没有完成的任务
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            // 如果等待被中断，强制关闭线程池
            if (executor != null) {
                executor.shutdownNow();
            }
            // 恢复中断
            Thread.currentThread().interrupt();
        }
    }
    
    // 预加载配置
    private static void preloadConfig() {
        if (executor.isShutdown()) {
            Log.w(TAG, "线程池已关闭，无法预加载");
            return;
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String config = fetchConfigWithRetry();
                    if (config != null && !config.isEmpty()) {
                        LocalConfig.saveConfigToCache(config);
                        Log.d(TAG, "预加载配置成功");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "预加载配置失败", e);
                }
            }
        });
    }
    
    // 获取配置
    public static String getConfig() {
        try {
            Log.d(TAG, "开始获取配置");
            
            // 如果最后工作的URL仍然有效，且刷新间隔内有效，直接返回缓存
            if (lastWorkingUrl != null && 
                System.currentTimeMillis() - lastSuccessTime < REFRESH_INTERVAL && 
                LocalConfig.isCacheValid()) {
                Log.d(TAG, "使用最近成功的URL: " + lastWorkingUrl);
                return LocalConfig.getCachedConfig();
            }
            
            // 网络不可用
            if (!isNetworkAvailable()) {
                Log.d(TAG, "网络不可用，使用缓存");
                String cachedConfig = LocalConfig.getCachedConfig();
                if (cachedConfig != null && !cachedConfig.isEmpty()) {
                    Log.d(TAG, "返回缓存配置: " + cachedConfig.length());
                    return cachedConfig;
                } else {
                    Log.d(TAG, "返回默认配置");
                    return LocalConfig.getDefaultConfig();
                }
            }
            
            // 异步更新缓存
            Log.d(TAG, "启动异步更新缓存");
            updateCacheAsync();
            
            // 返回当前缓存
            String cachedConfig = LocalConfig.getCachedConfig();
            if (cachedConfig != null && !cachedConfig.isEmpty()) {
                Log.d(TAG, "返回当前缓存配置: " + cachedConfig.length());
                return cachedConfig;
            } else {
                Log.d(TAG, "缓存为空，返回默认配置");
                return LocalConfig.getDefaultConfig();
            }
        } catch (Exception e) {
            Log.e(TAG, "获取配置异常", e);
            return LocalConfig.getDefaultConfig();
        }
    }
    
    // 异步更新缓存
    private static void updateCacheAsync() {
        if (executor.isShutdown()) {
            Log.w(TAG, "线程池已关闭，无法更新缓存");
            return;
        }
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String config = fetchConfigWithRetry();
                    if (config != null && !config.isEmpty()) {
                        LocalConfig.saveConfigToCache(config);
                        lastSuccessTime = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "更新缓存失败", e);
                }
            }
        });
    }
    
    // 尝试获取配置，带重试机制
    private static String fetchConfigWithRetry() {
        String result;
        
        // 优先尝试gitcode URL
        String gitcodeUrl = "https://raw.gitcode.com/sssddd55/344/raw/main/rjk";
        Log.d(TAG, "首先尝试gitcode URL: " + gitcodeUrl);
        result = fetchConfig(gitcodeUrl);
        if (result != null && !result.isEmpty()) {
            lastWorkingUrl = gitcodeUrl;
            return result;
        }
        
        // 如果gitcode URL失败，尝试上次成功的URL
        if (lastWorkingUrl != null && !lastWorkingUrl.equals(gitcodeUrl)) {
            Log.d(TAG, "gitcode URL失败，尝试上次成功的URL: " + lastWorkingUrl);
            result = fetchConfig(lastWorkingUrl);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        
        // 随机尝试URL列表中的其他URL
        String[] urlsToTry = backupMode ? BACKUP_URLS : SERVER_URLS;
        shuffleArray(urlsToTry);
        
        // 遍历尝试URL列表
        for (String url : urlsToTry) {
            if (url.equals(gitcodeUrl)) continue; // 跳过已尝试的gitcode URL
            result = fetchConfig(url);
            if (result != null && !result.isEmpty()) {
                lastWorkingUrl = url;
                backupMode = false;  // 重置标志
                return result;
            }
        }
        
        // 所有主要URL失败，切换到备用URL
        if (!backupMode) {
            backupMode = true;
            shuffleArray(BACKUP_URLS);
            for (String url : BACKUP_URLS) {
                if (url.equals(gitcodeUrl)) continue; // 跳过已尝试的gitcode URL
                result = fetchConfig(url);
                if (result != null && !result.isEmpty()) {
                    lastWorkingUrl = url;
                    return result;
                }
            }
        }
        
        // 所有URL都失败，返回null
        return null;
    }
    
    // 随机打乱数组
    private static void shuffleArray(String[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            String temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    // 从单个URL获取配置，使用try-with-resources确保资源关闭
    private static String fetchConfig(String urlString) {
        HttpURLConnection connection = null;
        
        try {
            // 准备连接请求指定的URL
            Log.d(TAG, "尝试连接到URL: " + urlString);
            
            // 设置随机超时，避免固定特征
            int timeout = 5000 + new Random().nextInt(3000);
            
            // 创建连接
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            
            // 随机选择User-Agent
            String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15"
            };
            connection.setRequestProperty("User-Agent", userAgents[new Random().nextInt(userAgents.length)]);
            
            // 检查响应
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // 使用try-with-resources自动关闭资源
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String config = response.toString();
                    if (isValidConfig(config)) {
                        // 配置有效，记录成功的URL
                        Log.d(TAG, "成功获取配置从URL: " + urlString);
                        // 只记录前100个字符(或更少)用于日志
                        String configSummary = config.length() > 100 ? config.substring(0, 100) + "..." : config;
                        Log.d(TAG, "配置内容: " + configSummary);
                        return config;
                    }
                }
            } else {
                Log.d(TAG, "URL返回非200状态码: " + urlString + ", code: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取配置异常: " + urlString, e);
        } finally {
            // 确保连接关闭
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "关闭连接异常", e);
                }
            }
        }
        return null;
    }
    
    // 验证配置是否有效
    private static boolean isValidConfig(String config) {
        if (config == null || config.trim().isEmpty()) {
            Log.d(TAG, "配置内容为null");
            return false;
        }

        // 检查必要的元素
        try {
            // 检查必要的标签和结构
            if (config.contains("对话配置") && config.contains("标签配置")) {
                Log.d(TAG, "配置格式正确");
                
                // 检查子元素
                boolean hasTitle = config.contains("标题") && config.contains("标题结束");
                boolean hasMessage = config.contains("内容") && config.contains("内容结束");
                
                if (hasTitle && hasMessage) {
                    Log.d(TAG, "配置包含所有必要元素");
                    return true;
                } else {
                    Log.d(TAG, "配置缺少必要元素：标题=" + hasTitle + "，内容=" + hasMessage);
                }
            } else if (config.contains("版本") && config.contains("更新") && 
                      config.contains("服务器配置") && config.contains("客户端配置")) {
                Log.d(TAG, "检测到兼容的配置格式，可能是XML或其他结构化格式");
                return true;
            } else {
                // 记录前100个字符用于调试
                String logContent = config.length() > 100 ? config.substring(0, 100) + "..." : config;
                Log.d(TAG, "配置不包含预期的必要元素: " + logContent);
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "验证配置时异常", e);
            return false;
        }
    }
    
    // 检查网络是否可用
    public static boolean isNetworkAvailable() {
        if (context == null) {
            return false;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
} 