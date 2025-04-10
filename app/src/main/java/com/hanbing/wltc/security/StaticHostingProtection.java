package com.hanbing.wltc.secure;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 静态托管保护
 * 从Gitee等静态托管源安全获取URL或配置数据
 * 包含反爬虫和反分析机制
 */
public class StaticHostingProtection {
    private static final String TAG = "StaticHostingProtection";
    private static Context mContext;
    
    // 仓库矩阵 (示例数据)
    private static final String[][] REPO_MATRIX = {
        {"username1/repo1", "branch1", "path/to/config1.txt"},
        {"username2/repo2", "branch2", "data/settings.json"},
        {"username3/repo3", "branch3", "resources/app.cfg"},
        {"username4/repo4", "main", "assets/data.bin"},
        {"username5/repo5", "release", "configs/system.dat"},
        {"username6/repo6", "develop", "static/backup.cfg"},
        {"username7/repo7", "master", "meta/core.bin"},
        {"username8/repo8", "stable", "storage/primary.txt"},
        {"username9/repo9", "v2", "public/external.json"},
        {"username10/repo10", "prod", "cache/settings.db"}
    };
    
    // 域名变体列表 (用于随机选择)
    private static final String[] DOMAIN_VARIANTS = {
        "gitee.com",
        "gitee.io",
        "raw.gitee.com", 
        "gitee.org", 
        "github.com",
        "raw.githubusercontent.com",
        "cdn.jsdelivr.net/gh",
        "fastly.jsdelivr.net/gh",
        "gcore.jsdelivr.net/gh",
        "gitlab.com"
    };
    
    // 诱饵域名 (用于发送干扰请求)
    private static final String[] DECOY_DOMAINS = {
        "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css",
        "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css",
        "https://unpkg.com/react@17/umd/react.production.min.js",
        "https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.min.js",
        "https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js",
        "https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js",
        "https://cdn.jsdelivr.net/npm/chart.js@3.7.0/dist/chart.min.js",
        "https://cdn.datatables.net/1.11.4/css/jquery.dataTables.min.css"
    };
    
    /**
     * 初始化
     */
    public static void init(Context context) {
        mContext = context.getApplicationContext();
    }
    
    /**
     * 安全地获取配置内容
     */
    public static String securelyFetchConfig() {
        try {
            final AtomicReference<String> result = new AtomicReference<>(null);
            
            // 在执行真实任务的同时发送诱饵请求
            executeWithDecoys(() -> {
                try {
                    String content = fetchSegmentedContent();
                    result.set(content);
                } catch (Exception e) {
                    Log.e(TAG, "获取分片内容时出错", e);
                }
            });
            
            return result.get();
        } catch (Exception e) {
            Log.e(TAG, "安全获取配置时出错", e);
            return null;
        }
    }
    
    /**
     * 选择仓库路径 (基于时间和设备指纹)
     */
    private static String[] selectRepositoryPath() {
        // 基于时间和设备指纹计算索引
        long timeComponent = System.currentTimeMillis() / (3600000 * 24); // 按天变化
        byte[] deviceFingerprint = getDeviceFingerprint();
        int index = Math.abs((int)((timeComponent ^
                           (deviceFingerprint.length > 0 ? deviceFingerprint[0] << 24 : 0) |
                           (deviceFingerprint.length > 1 ? deviceFingerprint[1] << 16 : 0)) %
                          REPO_MATRIX.length));
        return REPO_MATRIX[index];
    }
    
    /**
     * 获取时间编码的文件名
     */
    private static String getTimeEncodedFilename() {
        // 根据当前日期生成文件名
        Calendar cal = Calendar.getInstance();
        
        // 获取年和一年中的第几天
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        
        // 编码时间戳
        String encoded = encodeTimestamp(dayOfYear, year);
        return "data_" + encoded + ".txt";
    }
    
    /**
     * 编码时间戳 (简单混淆)
     */
    private static String encodeTimestamp(int dayOfYear, int year) {
        // 组合年和天
        int combined = (year * 1000) + dayOfYear;
        String basis = Integer.toString(combined, 36); // 转为36进制
        
        // 简单替换
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < basis.length(); i++) {
            char c = basis.charAt(i);
            // 数字和字母互换
            if (Character.isDigit(c)) {
                result.append((char)('a' + (c - '0')));
            } else {
                result.append((char)('0' + (c - 'a')));
            }
        }
        
        return result.toString();
    }
    
    /**
     * 获取设备指纹
     */
    private static byte[] getDeviceFingerprint() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // 收集设备信息
            baos.write(Build.FINGERPRINT.getBytes(StandardCharsets.UTF_8));
            // 注意: Build.SERIAL 在 Android Q 及以上版本可能不可用或返回固定值
            String serial = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? Build.SERIAL : "unknown";
            baos.write(serial.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.BOARD.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.BRAND.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.DEVICE.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.HARDWARE.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.ID.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.MANUFACTURER.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.MODEL.getBytes(StandardCharsets.UTF_8));
            baos.write(Build.PRODUCT.getBytes(StandardCharsets.UTF_8));

            // 添加应用签名信息
            if (mContext != null) {
                try {
                    PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(
                            mContext.getPackageName(), PackageManager.GET_SIGNATURES);
                    if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                        for (Signature signature : packageInfo.signatures) {
                            baos.write(signature.toByteArray());
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // 忽略包名未找到异常
                }
            }

            // 计算SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(baos.toByteArray());
        } catch (Exception e) {
            // 发生异常时返回随机字节数组
            byte[] fallback = new byte[32];
            new SecureRandom().nextBytes(fallback);
            return fallback;
        }
    }

    /**
     * 获取分片内容
     */
    private static String fetchSegmentedContent() throws Exception {
        // 获取分片矩阵
        int[][] segmentMatrix = getSegmentMatrix();
        ByteArrayOutputStream fullContent = new ByteArrayOutputStream();

        // 遍历矩阵，获取并解密每个片段
        for (int i = 0; i < segmentMatrix.length; i++) {
            String[] repoInfo = REPO_MATRIX[segmentMatrix[i][0]];
            String filename = "segment_" + segmentMatrix[i][1] + ".dat";

            // 从静态主机获取加密片段
            String encryptedSegment = fetchFromStaticHost(repoInfo, filename);
            byte[] segment = decryptSegment(encryptedSegment, i);

            // 写入完整内容流
            fullContent.write(segment, 0, segment.length);
        }

        return fullContent.toString(StandardCharsets.UTF_8);
    }

    /**
     * 获取分片矩阵 (基于设备指纹和时间)
     */
    private static int[][] getSegmentMatrix() {
        // 使用设备指纹和日期作为种子生成随机数
        byte[] fingerprint = getDeviceFingerprint();
        long timeKey = System.currentTimeMillis() / (86400000); // 按天变化

        // 使用混合后的种子初始化 SecureRandom
        SecureRandom random = new SecureRandom(mixSeed(fingerprint, timeKey));

        // 随机确定片段数量 (10-15个)
        int segmentCount = 10 + random.nextInt(6);
        int[][] matrix = new int[segmentCount][2];

        // 创建仓库索引列表并打乱
        List<Integer> repoIndices = new ArrayList<>();
        for (int i = 0; i < REPO_MATRIX.length; i++) {
            repoIndices.add(i);
        }
        // 打乱仓库索引顺序
        for (int i = 0; i < repoIndices.size(); i++) {
            int j = random.nextInt(repoIndices.size());
            int temp = repoIndices.get(i);
            repoIndices.set(i, repoIndices.get(j));
            repoIndices.set(j, temp);
        }

        // 填充矩阵 [仓库索引, 文件ID]
        for (int i = 0; i < segmentCount; i++) {
            matrix[i][0] = repoIndices.get(i % repoIndices.size()); // 循环使用仓库索引
            matrix[i][1] = random.nextInt(100) + 100; // 文件ID范围 100-199
        }

        return matrix;
    }

    /**
     * 混合种子 (设备指纹 + 时间)
     */
    private static byte[] mixSeed(byte[] fingerprint, long timeKey) {
        byte[] result = new byte[fingerprint.length + 8];
        System.arraycopy(fingerprint, 0, result, 0, fingerprint.length);

        // 添加时间部分 (long转byte[])
        for (int i = 0; i < 8; i++) {
            result[fingerprint.length + i] = (byte)(timeKey >> (i * 8));
        }

        return result;
    }

    /**
     * 从静态托管主机获取内容
     */
    private static String fetchFromStaticHost(String[] repoInfo, String filename) throws Exception {
        // 随机选择域名
        String domain = getRandomDomain();

        // 构建URL
        String url = buildUrlForDomain(domain, repoInfo, filename);

        // 发起HTTP请求
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(15000); // 15秒读取超时

            // 添加随机请求头
            addRandomRequestHeaders(connection);

            // 读取响应
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 解密片段
     */
    private static byte[] decryptSegment(String encryptedContent, int segmentIndex) throws Exception {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return new byte[0];
        }

        // Base64解码
        byte[] encryptedData = Base64.decode(encryptedContent, Base64.NO_WRAP);

        // 提取IV (前16字节)
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);
        System.arraycopy(encryptedData, 16, encrypted, 0, encrypted.length);

        // 派生密钥 (基于设备指纹和片段索引)
        byte[] deviceKey = getDeviceFingerprint();
        byte[] segmentKey = deriveSegmentKey(deviceKey, segmentIndex);

        // 使用AES解密
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(segmentKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(encrypted);
    }

    /**
     * 派生片段密钥
     */
    private static byte[] deriveSegmentKey(byte[] deviceKey, int segmentIndex) throws Exception {
        // 组合设备密钥和片段索引
        byte[] mixed = new byte[deviceKey.length + 4];
        System.arraycopy(deviceKey, 0, mixed, 0, deviceKey.length);

        // 添加片段索引 (大端序)
        mixed[deviceKey.length] = (byte)(segmentIndex >> 24);
        mixed[deviceKey.length + 1] = (byte)(segmentIndex >> 16);
        mixed[deviceKey.length + 2] = (byte)(segmentIndex >> 8);
        mixed[deviceKey.length + 3] = (byte)segmentIndex;

        // 使用SHA-256生成片段密钥
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(mixed);
    }

    /**
     * 获取随机域名
     */
    private static String getRandomDomain() {
        // 使用加权随机选择域名
        int index = getWeightedRandomIndex(DOMAIN_VARIANTS.length);
        return DOMAIN_VARIANTS[index];
    }

    /**
     * 获取加权随机索引
     */
    private static int getWeightedRandomIndex(int length) {
        SecureRandom random = new SecureRandom();

        // 使用幂函数使选择偏向列表前面的项
        double value = random.nextDouble();
        value = Math.pow(value, 1.5); // 指数 > 1 使概率向低索引倾斜

        return (int)(value * length);
    }

    /**
     * 为特定域名构建URL
     */
    private static String buildUrlForDomain(String domain, String[] repoInfo, String filename) {
        String username = repoInfo[0].split("/")[0];
        String repo = repoInfo[0].split("/")[1];
        String branch = repoInfo[1];
        String path = repoInfo[2];

        String url;

        // 根据不同托管平台构建URL格式
        if (domain.equals("gitee.com") || domain.equals("github.com") || domain.equals("gitlab.com")) {
            url = "https://" + domain + "/" + username + "/" + repo + "/raw/" + branch + "/" + path + "/" + filename;
        } else if (domain.startsWith("raw.")) { // raw.gitee.com, raw.githubusercontent.com
            url = "https://" + domain + "/" + username + "/" + repo + "/" + branch + "/" + path + "/" + filename;
        } else if (domain.contains("jsdelivr.net")) { // cdn.jsdelivr.net, fastly.jsdelivr.net, gcore.jsdelivr.net
            url = "https://" + domain + "/" + username + "/" + repo + "@" + branch + "/" + path + "/" + filename;
        } else {
            // 默认格式 (可能适用于 gitee.io 等)
            url = "https://" + domain + "/" + username + "/" + repo + "/" + branch + "/" + path + "/" + filename;
        }

        // 添加缓存破坏参数
        url += "?t=" + System.currentTimeMillis() + "&r=" + new SecureRandom().nextInt(10000);

        return url;
    }

    /**
     * 添加随机请求头
     */
    private static void addRandomRequestHeaders(HttpURLConnection connection) {
        SecureRandom random = new SecureRandom();

        // 随机User-Agent
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
        };
        connection.setRequestProperty("User-Agent", userAgents[random.nextInt(userAgents.length)]);

        // 随机Accept类型
        String[] acceptTypes = {
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "application/json,text/plain,*/*;q=0.9",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "image/webp,image/apng,image/*,*/*;q=0.8"
        };
        connection.setRequestProperty("Accept", acceptTypes[random.nextInt(acceptTypes.length)]);

        // 随机语言
        String[] languages = {
            "en-US,en;q=0.9",
            "zh-CN,zh;q=0.9,en;q=0.8",
            "en-GB,en;q=0.7",
            "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7"
        };
        connection.setRequestProperty("Accept-Language", languages[random.nextInt(languages.length)]);

        // 随机添加其他常见头部
        if (random.nextBoolean()) {
            connection.setRequestProperty("Cache-Control", "max-age=0");
        }

        connection.setRequestProperty("Connection", "keep-alive");

        if (random.nextBoolean()) {
            connection.setRequestProperty("DNT", "1"); // Do Not Track
        }

        if (random.nextBoolean()) {
            connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        }

        // 随机添加Sec-Fetch-*头部
        String[] secHeaders = {
            "Sec-Fetch-Dest", "Sec-Fetch-Mode", "Sec-Fetch-Site", "Sec-Fetch-User"
        };
        String[] secValues = {
            "document", "navigate", "none", "?1"
        };

        for (int i = 0; i < secHeaders.length; i++) {
            if (random.nextBoolean()) {
                connection.setRequestProperty(secHeaders[i], secValues[i]);
            }
        }
    }

    /**
     * 执行实际任务并发送诱饵请求
     */
    private static void executeWithDecoys(final Runnable actualTask) {
        // 随机生成诱饵请求数量 (20-30个)
        SecureRandom random = new SecureRandom();
        int decoyCount = 20 + random.nextInt(11); // 20 to 30

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(decoyCount + 1);

        // 提交前半部分诱饵请求
        for (int i = 0; i < decoyCount / 2; i++) {
            executor.submit(() -> sendDecoyRequest());
        }

        // 提交实际任务
        Future<?> actual = executor.submit(actualTask);

        // 提交后半部分诱饵请求
        for (int i = 0; i < (decoyCount + 1) / 2; i++) { // Ensure total decoys = decoyCount
            executor.submit(() -> sendDecoyRequest());
        }

        // 等待实际任务完成 (设置超时)
        try {
            actual.get(30, TimeUnit.SECONDS); // 30秒超时
        } catch (Exception e) {
            Log.e(TAG, "获取内容失败或超时", e);
        } finally {
            executor.shutdown(); // 关闭线程池
        }
    }

    /**
     * 发送诱饵请求
     */
    private static void sendDecoyRequest() {
        try {
            SecureRandom random = new SecureRandom();
            String decoyUrl = DECOY_DOMAINS[random.nextInt(DECOY_DOMAINS.length)];

            // 添加随机参数防止缓存
            decoyUrl += "?t=" + System.currentTimeMillis() + "&r=" + random.nextInt(10000);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(decoyUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5秒连接超时
                connection.setReadTimeout(5000);  // 5秒读取超时

                // 添加随机请求头
                addRandomRequestHeaders(connection);

                // 发起连接并获取响应码 (不读取内容)
                int responseCode = connection.getResponseCode();
                // Log.d(TAG, "Decoy request to " + decoyUrl + " returned " + responseCode);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // 随机延迟
            Thread.sleep(50 + random.nextInt(200));

        } catch (Exception e) {
            // 忽略诱饵请求的错误
            // Log.w(TAG, "Decoy request failed: " + e.getMessage());
        }
    }

    /**
     * 验证内容 (示例，可能未使用)
     */
    private static boolean validateContent(String content, byte[] deviceData) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        try {
            // 假设内容中包含签名注释 <!-- signature -->
            Pattern validationPattern = Pattern.compile("<!--\\s*([a-zA-Z0-9+/=]+)\\s*-->");
            Matcher matcher = validationPattern.matcher(content);

            if (matcher.find()) {
                String encodedData = matcher.group(1);
                byte[] signatureData = Base64.decode(encodedData, Base64.DEFAULT);

                // 计算内容哈希 (结合设备数据)
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(deviceData);
                byte[] contentHash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

                // 比较签名 (使用固定时间比较防止时序攻击)
                return slowEquals(signatureData, contentHash);
            }

            return false; // 未找到签名
        } catch (Exception e) {
            Log.e(TAG, "验证内容时出错", e);
            return false;
        }
    }

    /**
     * 固定时间字节数组比较 (防止时序攻击)
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }

        return result == 0;
    }
}
