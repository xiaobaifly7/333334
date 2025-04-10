package com.hanbing.wltc.core.protection;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * 设备身份标识工具
 * 用于生成和管理设备唯一标识符
 */
public class DeviceIdentity {

    private static final String TAG = "DeviceIdentity";
    
    // 设备ID缓存
    private static String cachedDeviceId = null;
    
    // 哈希盐值
    private static final String HASH_SALT = "Hb7#9pL2q5*K!";
    
    /**
     * 获取设备唯一标识符
     * @param context 应用上下文
     * @return 设备唯一标识符
     */
    public static String getDeviceId(Context context) {
        // 检查缓存
        if (cachedDeviceId != null && !cachedDeviceId.isEmpty()) {
            return cachedDeviceId;
        }
        
        try {
            // 构建多个设备信息
            StringBuilder deviceInfoBuilder = new StringBuilder();
            
            // 添加 Android ID
            String androidId = Settings.Secure.getString(context.getContentResolver(), 
                    Settings.Secure.ANDROID_ID);
            deviceInfoBuilder.append(androidId == null ? "" : androidId);
            
            // 添加设备信息
            deviceInfoBuilder.append(Build.BOARD);
            deviceInfoBuilder.append(Build.BRAND);
            deviceInfoBuilder.append(Build.DEVICE);
            deviceInfoBuilder.append(Build.HARDWARE);
            deviceInfoBuilder.append(Build.MANUFACTURER);
            deviceInfoBuilder.append(Build.MODEL);
            deviceInfoBuilder.append(Build.PRODUCT);
            deviceInfoBuilder.append(Build.SERIAL);
            
            // 尝试获取 IMEI (需要权限)
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String imei = telephonyManager.getDeviceId();
                if (imei != null && !imei.isEmpty()) {
                    deviceInfoBuilder.append(imei);
                }
            } catch (Exception e) {
                // 忽略权限异常
            }
            
            // 添加哈希盐值增强安全性
            deviceInfoBuilder.append(HASH_SALT);
            
            // 应用 SHA-256 哈希算法
            String deviceInfo = deviceInfoBuilder.toString();
            if (deviceInfo.isEmpty()) {
                // 如果无法获取任何设备信息，则生成随机UUID
                return UUID.randomUUID().toString();
            }
            
            // 计算哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceInfo.getBytes("UTF-8"));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 缓存结果
            cachedDeviceId = hexString.toString();
            return cachedDeviceId;
            
        } catch (Exception e) {
            // 出现异常时返回随机UUID
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 重置设备ID缓存
     */
    public static void resetCache() {
        cachedDeviceId = null;
    }
    
    /**
     * 检查设备ID是否已变更
     * @param context 应用上下文
     * @param storedId 存储的设备ID
     * @return 是否已变更
     */
    public static boolean hasDeviceChanged(Context context, String storedId) {
        if (storedId == null || storedId.isEmpty()) {
            return true;
        }
        
        String currentId = getDeviceId(context);
        return !storedId.equals(currentId);
    }
}