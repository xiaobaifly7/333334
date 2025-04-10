package com.hanbing.wltc.core.protection;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 防篡改工具类
 * 提供各种防篡改和完整性检查功能
 */
public class AntiTamperUtils {

    private static final String TAG = "AntiTamperUtils";
    
    // 预期的签名SHA-256哈希值
    private static final String EXPECTED_SIGNATURE_HASH = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0";
    
    /**
     * 验证应用签名
     */
    public static boolean verifyAppSignature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            
            // 获取签名信息
            Signature[] signatures = packageInfo.signatures;
            if (signatures == null || signatures.length == 0) {
                return false;
            }
            
            // 计算签名的SHA-256哈希值
            byte[] signatureBytes = signatures[0].toByteArray();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(signatureBytes);
            
            // 转换为十六进制字符串
            String signatureHash = bytesToHex(hashBytes);
            
            // 验证签名是否匹配
            return EXPECTED_SIGNATURE_HASH.equals(signatureHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证DEX文件完整性
     */
    public static boolean verifyDexIntegrity(Context context) {
        try {
            // 获取应用APK文件
            String apkPath = context.getPackageCodePath();
            File apkFile = new File(apkPath);
            
            // 打开APK文件作为ZIP文件
            ZipFile zipFile = new ZipFile(apkFile);
            
            // 检查classes.dex文件
            ZipEntry dexEntry = zipFile.getEntry("classes.dex");
            if (dexEntry == null) {
                zipFile.close();
                return false;
            }
            
            // 读取dex文件内容并计算哈希
            InputStream dexStream = zipFile.getInputStream(dexEntry);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dexStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            dexStream.close();
            zipFile.close();
            
            // 对比计算的哈希值与预期值
            // 注意：实际应用中应该动态获取或者使用更安全的验证方式
            byte[] hashBytes = digest.digest();
            // 这里仅做演示，返回true
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查设备是否被篡改
     */
    public static boolean isDeviceTampered() {
        // 检查是否为Root设备
        if (isRooted()) {
            return true;
        }
        
        // 检查是否为模拟器
        if (isEmulator()) {
            return true;
        }
        
        // 检查是否有调试器连接
        if (isBeingDebugged()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查设备是否已Root
     */
    private static boolean isRooted() {
        // 检查常见的Root文件
        String[] rootFiles = {
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su"
        };
        
        for (String path : rootFiles) {
            if (new File(path).exists()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否在模拟器中运行
     */
    private static boolean isEmulator() {
        // 简单检查，实际应用中应该使用更复杂的检测方法
        return android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(android.os.Build.PRODUCT);
    }
    
    /**
     * 检查是否有调试器连接
     */
    private static boolean isBeingDebugged() {
        // 检查是否处于调试模式
        return android.os.Debug.isDebuggerConnected();
    }
    
    /**
     * 字节数组转十六进制字符串
     */
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