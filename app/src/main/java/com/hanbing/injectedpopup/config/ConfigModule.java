package com.hanbing.injectedpopup.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 配置模块 (兼容性优先版 v3.1)
 * 负责从多个引导线路获取加密的真实URL列表，再从真实URL获取加密的主配置，
 * 并在Java层进行解密、解析和缓存。
 * 安全性高度依赖最终的Dex加固。
 */
public class ConfigModule {

    private static final String TAG = "ConfigModule";
    // --- KDF and Crypto Constants ---
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITERATIONS = 10000; // 迭代次数
    private static final int KDF_KEY_LENGTH_BITS = 256; // AES-256 密钥长度
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // GCM 标准 IV 长度
    private static final int GCM_TAG_LENGTH_BITS = 128; // GCM 认证标签长度
    private static final int URL_KEY_LENGTH_BYTES = 16; // URL 解密/混淆用密钥长度 (示例)

    // --- Embedded Salt/Secret Parts (依赖 Dex 加固保护) ---
    // TODO: Replace placeholders with actual obfuscated/split secret parts
    // 这些常量需要分散存储在不同地方，并依赖 Dex 加固保护
    private static final String SALT_PART_A = "OBFUSCATED_SALT_PART_A_Long_Random_String_1";
    private static final byte[] SALT_PART_B = {0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F}; // Example
    private static final String URL_XOR_KEY_PART = "OBFUSCATED_XOR_KEY_PART_Another_Random_String_2"; // Example for URL XOR decryption
    // ----------------------------------------------------

    private static final String PREFS_NAME = "injected_popup_prefs";
    private static final String KEY_CACHED_CONFIG = "cached_config_json"; // Stores decrypted JSON
    private static final String KEY_IGNORED_VERSION = "ignored_version";
    private static final String KEY_LAST_FETCH_TIME = "last_fetch_time";
    private static final long CACHE_EXPIRY_MS = 6 * 60 * 60 * 1000; // 6 小时缓存有效期

    // --- !!! 关键配置点 !!! ---
    // TODO: 将实际的 CDN/托管 URL (指向加密引导文件) 的 *密文* (Base64+XOR处理后) 替换下面的占位符
    // TODO: 依赖 Dex 加固保护此列表和 URL 解密逻辑/密钥
    private static final String[] BOOTSTRAP_URLS_CIPHERTEXT = {
            "PLACEHOLDER_ENCRYPTED_BOOTSTRAP_URL_1_BASE64_XOR", // 示例引导 URL 1 (加密/混淆后)
            "PLACEHOLDER_ENCRYPTED_BOOTSTRAP_URL_2_BASE64_XOR", // 示例引导 URL 2 (加密/混淆后)
            "PLACEHOLDER_ENCRYPTED_BOOTSTRAP_URL_3_BASE64_XOR"  // 示例引导 URL 3 (加密/混淆后)
            // 添加更多备用引导线路 URL...
    };

    // TODO: 将加密后的内置默认配置 (Base64 编码的 AES-GCM 密文) 替换下面的占位符
    // 使用与主配置相同的加密方式和密钥派生逻辑
    private static final String ENCRYPTED_DEFAULT_CONFIG_BASE64 = "PLACEHOLDER_ENCRYPTED_DEFAULT_CONFIG_JSON_BASE64";
    // --------------------------

    private static volatile ConfigModule instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final ExecutorService networkExecutor;
    private final AtomicReference<String> memoryCachedConfig = new AtomicReference<>(null);
    // sortedBootstrapUrls 现在存储引导 URL 的延迟信息
    private final AtomicReference<List<UrlLatency>> sortedBootstrapUrls = new AtomicReference<>(null);
    private final Random random = new Random();

    private static class UrlLatency {
        String url;
        long latency; // in milliseconds

        UrlLatency(String url, long latency) {
            this.url = url;
            this.latency = latency;
        }
    }

    private ConfigModule(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 使用固定线程池进行网络请求和测速
        this.networkExecutor = Executors.newFixedThreadPool(Math.min(BOOTSTRAP_URLS_CIPHERTEXT.length, 4)); // 基于引导 URL 数量
        loadCache();
        // 异步测速并排序引导 URL
        measureAndSortBootstrapUrlsAsync();
    }

    public static ConfigModule getInstance(Context context) {
        if (instance == null) {
            synchronized (ConfigModule.class) {
                if (instance == null) {
                    instance = new ConfigModule(context);
                }
            }
        }
        return instance;
    }

    /**
     * 从多线路获取加密的配置数据 (字节数组)
     * 包含引导、双重解密、测速、排序、故障切换和简单重试逻辑
     * @return 加密的主配置字节数组，如果所有线路都失败则返回内置的默认加密配置
     */
    public byte[] fetchEncryptedConfig() {
        // 1. 获取解密后的引导 URL 列表
        List<String> bootstrapUrls = decryptBootstrapUrls();
        if (bootstrapUrls == null || bootstrapUrls.isEmpty()) {
            Log.e(TAG, "Failed to get bootstrap URLs.");
            return getDefaultEncryptedConfig(); // 无法获取引导 URL，尝试默认配置
        }

        // 2. 尝试从引导 URL 获取加密的引导文件内容
        byte[] encryptedBootstrapContent = fetchFromBootstrapUrls(bootstrapUrls);
        if (encryptedBootstrapContent == null || encryptedBootstrapContent.length == 0) {
            Log.e(TAG, "Failed to fetch bootstrap file content.");
            return getDefaultEncryptedConfig(); // 获取引导文件失败，尝试默认配置
        }
        Log.d(TAG, "Fetched encrypted bootstrap content (" + encryptedBootstrapContent.length + " bytes).");

        // 3. 解密引导文件获取真实配置 URL 列表
        List<String> realConfigUrls = decryptBootstrapContent(encryptedBootstrapContent);
        if (realConfigUrls == null || realConfigUrls.isEmpty()) {
            Log.e(TAG, "Failed to decrypt bootstrap file or get real config URLs.");
            return getDefaultEncryptedConfig(); // 解密失败，尝试默认配置
        }
        Log.d(TAG, "Decrypted real config URLs: " + realConfigUrls.size());

        // 4. 从真实配置 URL 获取加密的主配置
        byte[] mainConfigData = fetchFromRealUrls(realConfigUrls);
        if (mainConfigData != null && mainConfigData.length > 0) {
             Log.i(TAG, "Successfully fetched main encrypted config.");
            return mainConfigData;
        } else {
            // 如果从真实 URL 获取失败，也尝试默认配置
            Log.e(TAG, "Failed to fetch main config from real URLs.");
            return getDefaultEncryptedConfig();
        }
    }

    /**
     * 尝试从单个 URL 获取数据，带重试
     */
    private byte[] attemptFetch(String urlString, int maxAttempts) {
        // 添加 User-Agent 列表
        final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        };

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                // 加入随机延迟
                if (attempt > 1) {
                    Thread.sleep(random.nextInt(300) + 200 * attempt);
                }

                Log.d(TAG, "Attempt " + attempt + " to fetch from: " + urlString);
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000); // 8 秒连接超时
                connection.setReadTimeout(15000);  // 15 秒读取超时
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "*/*");
                // 设置随机 User-Agent
                connection.setRequestProperty("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)]);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    byte[] dataChunk = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(dataChunk, 0, dataChunk.length)) != -1) {
                        buffer.write(dataChunk, 0, bytesRead);
                    }
                    buffer.flush();
                    return buffer.toByteArray();
                } else {
                    Log.w(TAG, "Failed attempt " + attempt + " for " + urlString + ". Response code: " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException during attempt " + attempt + " for " + urlString, e);
            } catch (InterruptedException e) {
                 Log.w(TAG, "Fetch attempt interrupted", e);
                 Thread.currentThread().interrupt();
                 return null;
            } finally {
                if (inputStream != null) try { inputStream.close(); } catch (IOException ignored) {}
                if (buffer != null) try { buffer.close(); } catch (IOException ignored) {}
                if (connection != null) connection.disconnect();
            }
        }
        return null; // 所有尝试失败
    }

    // --- URL Decryption/Deobfuscation (Java Implementation) ---

    /**
     * 解密/解混淆引导 URL 列表 - **Java 实现**
     * @return 解密后的引导 URL 列表，失败返回 null
     */
    private List<String> decryptBootstrapUrls() {
        Log.d(TAG, "Decrypting bootstrap URLs...");
        List<String> decryptedUrls = new ArrayList<>();
        byte[] urlKey = null;
        try {
            // 1. 获取用于解密 URL 的密钥
            urlKey = getBootstrapUrlDecryptionKey(); // KDF 派生
            if (urlKey == null) throw new SecurityException("Failed to derive bootstrap URL key");

            // 2. 循环解密 BOOTSTRAP_URLS_CIPHERTEXT 中的每个 URL
            for (String encryptedUrlBase64 : BOOTSTRAP_URLS_CIPHERTEXT) {
                // --- Java 解密/解混淆逻辑 (示例: Base64 + XOR) ---
                // TODO: 替换为您实际选择的 URL 加密/混淆算法
                String decryptedUrl = decryptUrlSimpleXor(encryptedUrlBase64, urlKey);
                // --- 实现结束 ---

                if (decryptedUrl != null && !decryptedUrl.isEmpty() && decryptedUrl.startsWith("https://")) {
                    decryptedUrls.add(decryptedUrl);
                } else {
                    Log.w(TAG, "Failed to decrypt or invalid bootstrap URL: " + encryptedUrlBase64);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting bootstrap URLs", e);
            return null; // 返回 null 表示失败
        } finally {
             // 清理密钥
             if (urlKey != null) java.util.Arrays.fill(urlKey, (byte) 0);
        }
        return decryptedUrls.isEmpty() ? null : decryptedUrls;
    }

    /**
     * 获取用于解密引导 URL 的密钥 - **Java 实现 KDF**
     * @return 密钥字节数组，失败返回 null
     */
    private byte[] getBootstrapUrlDecryptionKey() {
        Log.d(TAG, "Deriving bootstrap URL decryption key using Java KDF...");
        // --- Java KDF 实现 ---
        // 使用 PBKDF2WithHmacSHA256
        // 输入应结合设备信息、包名、代码中混淆的片段等
        // 返回派生的密钥 (例如 16 字节 for 简单 XOR 或 AES)
        return deriveKeyWithJavaKDF("URL_KEY", URL_KEY_LENGTH_BYTES); // 使用常量定义密钥长度
        // --- 实现结束 ---
    }

    /**
     * 简单的 URL 解密示例 (Base64 + XOR) - **根据需要替换**
     */
    private String decryptUrlSimpleXor(String encryptedBase64, byte[] key) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty() || key == null || key.length == 0) {
            return null;
        }
        byte[] decryptedBytes = null;
        try {
            byte[] encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT);
            decryptedBytes = new byte[encryptedBytes.length];
            for (int i = 0; i < encryptedBytes.length; i++) {
                decryptedBytes[i] = (byte) (encryptedBytes[i] ^ key[i % key.length]);
            }
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Simple XOR decryption failed", e);
            return null;
        } finally {
            // 清理中间数据
            if (decryptedBytes != null) Arrays.fill(decryptedBytes, (byte) 0);
        }
    }

    // --- Bootstrap Content Decryption (Java Implementation) ---

    /**
     * 解密引导文件内容，获取真实配置 URL 列表 - **Java 实现**
     * @param encryptedBootstrapContent 加密的引导文件内容 (假设是 Base64 编码的 AES-GCM 密文)
     * @return 真实配置 URL 列表，失败返回 null
     */
    private List<String> decryptBootstrapContent(byte[] encryptedBootstrapContent) {
        Log.d(TAG, "Decrypting bootstrap content...");
        byte[] bootstrapKey = null;
        byte[] decryptedBytes = null;
        try {
            // 1. 获取用于解密引导文件的密钥
            bootstrapKey = getBootstrapContentDecryptionKey(); // KDF 派生
            if (bootstrapKey == null) throw new SecurityException("Failed to derive bootstrap content key");

            // 2. 使用 AES-GCM 解密 (Java 实现)
            // --- Java AES-GCM 解密逻辑 ---
            decryptedBytes = decryptAesGcmJava(encryptedBootstrapContent, bootstrapKey);
            // --- 实现结束 ---

            if (decryptedBytes != null && decryptedBytes.length > 0) {
                String decryptedUrlListString = new String(decryptedBytes, StandardCharsets.UTF_8);
                // 按换行符分割 URL
                String[] urls = decryptedUrlListString.split("\\s*\n\\s*"); // 按换行符分割，忽略前后空格
                List<String> urlList = new ArrayList<>(Arrays.asList(urls));
                // 移除空行
                urlList.removeAll(Collections.singleton(""));
                if (!urlList.isEmpty()) {
                    return urlList;
                } else {
                     Log.e(TAG, "Decrypted bootstrap content is empty or contains no URLs.");
                }
            } else {
                 Log.e(TAG, "Decryption of bootstrap content failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting bootstrap content", e);
        } finally {
             // 清理密钥和解密后的字节
             if (bootstrapKey != null) java.util.Arrays.fill(bootstrapKey, (byte) 0);
             if (decryptedBytes != null) java.util.Arrays.fill(decryptedBytes, (byte) 0);
        }
        return null; // 返回 null 表示失败
    }

     /**
     * 获取用于解密引导文件内容的密钥 - **Java 实现 KDF**
     * @return 密钥字节数组，失败返回 null
     */
    private byte[] getBootstrapContentDecryptionKey() {
        Log.d(TAG, "Deriving bootstrap content decryption key using Java KDF...");
        // --- Java KDF 实现 ---
        // 使用与解密 URL 不同的因子或参数
        return deriveKeyWithJavaKDF("BOOTSTRAP_KEY", KDF_KEY_LENGTH_BITS / 8); // 派生 AES-256 密钥
        // --- 实现结束 ---
    }

    // --- Main Config Decryption (Java Implementation) ---

    /**
     * 获取用于解密主配置的密钥 - **Java 实现 KDF**
     * @return 主配置解密密钥字节数组，如果失败返回 null
     */
    public byte[] getMainConfigDecryptionKey() {
        Log.d(TAG, "Getting main config decryption key using Java KDF...");
        // --- Java KDF 实现 ---
        // 使用与引导解密不同的因子或参数
        return deriveKeyWithJavaKDF("CONFIG_KEY", KDF_KEY_LENGTH_BITS / 8); // 派生 AES-256 密钥
        // --- 实现结束 ---
    }

    /**
     * 解密主配置数据 - **Java 实现**
     * @param encryptedData 加密的配置数据 (Base64 编码的 AES-GCM 密文)
     * @param key 解密密钥
     * @return 解密后的 JSON 字符串，如果失败返回 null
     */
    public String decryptMainConfig(byte[] encryptedData, byte[] key) {
        Log.d(TAG, "Decrypting main config data...");
        byte[] decryptedBytes = null;
        byte[] keyCopy = null; // Copy key for cleanup
        try {
            if (key != null) keyCopy = Arrays.copyOf(key, key.length);
            // --- Java AES-GCM 解密逻辑 ---
            decryptedBytes = decryptAesGcmJava(encryptedData, keyCopy);
            // --- 实现结束 ---

            if (decryptedBytes != null && decryptedBytes.length > 0) {
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
             Log.e(TAG, "Failed to convert decrypted bytes to String", e);
        } finally {
             // 清理敏感数据
             if (decryptedBytes != null) java.util.Arrays.fill(decryptedBytes, (byte) 0);
             if (keyCopy != null) java.util.Arrays.fill(keyCopy, (byte) 0); // Clean the copy
        }
        return null;
    }

    // --- KDF and Decryption Java Implementations ---

    /**
     * Java 实现的密钥派生函数 (示例 - PBKDF2) - **需要完善和保护**
     * @param purpose 密钥用途标识 (用于生成不同的盐值部分)
     * @param keyLengthBytes 期望的密钥长度（字节）
     * @return 派生的密钥字节数组，失败返回 null
     */
    private byte[] deriveKeyWithJavaKDF(String purpose, int keyLengthBytes) {
        byte[] derivedKey = null;
        char[] password = null;
        byte[] salt = null;
        byte[] embeddedSaltPart2 = null;
        byte[] purposeBytes = null;
        SecretKey tmp = null;

        try {
            // 1. 准备 KDF 输入 "密码" (基于设备信息和嵌入片段)
            String deviceInfoHash = getDeviceInfoHash(); // 获取设备信息哈希
            String embeddedSaltPart1 = getSaltPartA(); // 获取混淆/加固保护的片段 A
            // 组合成 "密码" - 这里的组合方式需要保密，依赖加固
            if (deviceInfoHash == null || embeddedSaltPart1 == null) {
                Log.e(TAG, "KDF input factor is null.");
                return null;
            }
            password = (deviceInfoHash + purpose + embeddedSaltPart1).toCharArray();

            // 2. 准备 KDF 输入 "盐值" (基于其他嵌入片段和用途)
            embeddedSaltPart2 = getSaltPartB(); // 获取混淆/加固保护的片段 B
            if (embeddedSaltPart2 == null) {
                 Log.e(TAG, "KDF salt factor is null.");
                 if (password != null) Arrays.fill(password, '\0');
                 return null;
            }
            purposeBytes = purpose.getBytes(StandardCharsets.UTF_8);
            salt = combineBytes(purposeBytes, embeddedSaltPart2);
            if (salt == null) {
                 Log.e(TAG, "Failed to combine salt for KDF.");
                 if (password != null) Arrays.fill(password, '\0');
                 if (embeddedSaltPart2 != null) Arrays.fill(embeddedSaltPart2, (byte) 0);
                 if (purposeBytes != null) Arrays.fill(purposeBytes, (byte) 0);
                 return null;
            }


            // 3. 执行 PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, KDF_ITERATIONS, keyLengthBytes * 8); // keyLength in bits
            tmp = factory.generateSecret(spec);
            derivedKey = tmp.getEncoded();

            if (derivedKey != null && derivedKey.length == keyLengthBytes) {
                // 返回密钥副本，防止外部修改影响内部状态（如果需要）
                return Arrays.copyOf(derivedKey, derivedKey.length);
            } else {
                Log.e(TAG, "KDF derived key is null or has incorrect length.");
                return null;
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "KDF Error for purpose " + purpose, e);
            return null;
        } finally {
            // 4. 清理敏感中间数据
            if (password != null) Arrays.fill(password, '\0');
            if (salt != null) Arrays.fill(salt, (byte) 0);
            if (embeddedSaltPart2 != null) Arrays.fill(embeddedSaltPart2, (byte) 0);
            if (purposeBytes != null) Arrays.fill(purposeBytes, (byte) 0);
            // derivedKey 在外部使用后也应被清理
            // SecretKey 对象不需要手动清理
        }
    }

    /**
     * Java 实现的 AES-GCM 解密 (示例) - **需要完善和保护**
     * @param encryptedDataWithIvTag Base64 解码后的加密数据 (IV + 密文 + Tag)
     * @param key 密钥 (例如 32 字节 for AES-256)
     * @return 解密后的明文字节数组，失败返回 null
     */
    private byte[] decryptAesGcmJava(byte[] encryptedDataWithIvTag, byte[] key) {
        // 验证输入
        if (encryptedDataWithIvTag == null || encryptedDataWithIvTag.length <= GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
             Log.e(TAG, "Invalid encrypted data length for AES-GCM.");
            return null;
        }
        // 确保密钥长度正确 (AES-256 需要 32 字节)
        if (key == null || key.length != KDF_KEY_LENGTH_BITS / 8) {
             Log.e(TAG, "Invalid key length for AES-" + KDF_KEY_LENGTH_BITS);
            return null;
        }

        byte[] iv = null;
        byte[] decryptedData = null;
        SecretKeySpec keySpec = null;
        GCMParameterSpec gcmSpec = null;
        Cipher cipher = null;

        try {
            // 1. 分离 IV 和密文+Tag
            iv = Arrays.copyOfRange(encryptedDataWithIvTag, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertextWithTag = Arrays.copyOfRange(encryptedDataWithIvTag, GCM_IV_LENGTH_BYTES, encryptedDataWithIvTag.length);

            // 2. 初始化 Cipher
            cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            keySpec = new SecretKeySpec(key, AES_ALGORITHM);
            gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // 3. 执行解密（包含认证标签验证）
            decryptedData = cipher.doFinal(ciphertextWithTag);

            return decryptedData; // 返回解密后的数据

        } catch (Exception e) {
            Log.e(TAG, "AES-GCM decryption failed", e);
            return null; // 解密失败
        } finally {
             // 4. 清理 IV (decryptedData 在外部清理)
             if (iv != null) Arrays.fill(iv, (byte) 0);
             // keySpec 和 gcmSpec 不需要手动清理
        }
    }

    // --- Helper Methods for KDF ---

    /** 获取设备信息哈希 (示例) */
    private String getDeviceInfoHash() {
        // TODO: 实现更健壮的设备信息获取和哈希逻辑
        // 注意处理权限和 Android 版本兼容性
        byte[] hash = null;
        try {
            String deviceInfo = Build.BRAND + Build.MODEL + Build.MANUFACTURER + appContext.getPackageName() + Build.FINGERPRINT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(deviceInfo.getBytes(StandardCharsets.UTF_8));
            // 返回哈希的 Base64 编码
             return Base64.encodeToString(hash, Base64.NO_WRAP | Base64.URL_SAFE); // Use URL safe Base64
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device info hash", e);
            return "default_device_hash_placeholder"; // 提供一个默认值
        } finally {
             // 清理中间数据
             if (hash != null) Arrays.fill(hash, (byte) 0);
        }
    }

    /** 获取盐值片段 A (依赖 Dex 加固保护) */
    private String getSaltPartA() {
        // TODO: 实现从混淆/加密存储中获取片段的逻辑
        // 示例: return SimpleObfuscator.reveal(SALT_PART_A);
        // 临时返回占位符，实际需要替换并依赖加固
        // **必须**确保此方法返回的值是稳定的
        return SALT_PART_A;
    }

    /** 获取盐值片段 B (依赖 Dex 加固保护) */
    private byte[] getSaltPartB() {
        // TODO: 实现从混淆/加密存储中获取片段的逻辑
        // 示例: return SimpleObfuscator.revealBytes(SALT_PART_B);
        // 临时返回占位符，实际需要替换并依赖加固
        // **必须**确保此方法返回的值是稳定的
        return Arrays.copyOf(SALT_PART_B, SALT_PART_B.length); // 返回副本防止外部修改
    }

    /** 组合字节数组 (示例) */
    private byte[] combineBytes(byte[] a, byte[] b) {
        if (a == null || b == null) return null;
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    // --- Fetching Logic ---

    /**
     * 从多个引导 URL 中获取加密的引导文件内容
     */
    private byte[] fetchFromBootstrapUrls(List<String> bootstrapUrls) {
        Log.d(TAG, "Fetching from bootstrap URLs...");
        List<UrlLatency> currentSortedBootstrapUrls = sortedBootstrapUrls.get(); // 使用 sortedBootstrapUrls 缓存引导 URL 延迟
        List<String> urlsToTry;

        // 优先使用排序后的引导 URL
        if (currentSortedBootstrapUrls != null && !currentSortedBootstrapUrls.isEmpty()) {
             // 确保排序列表中的 URL 仍然在当前的 bootstrapUrls 中 (可能引导文件更新了)
             List<String> validSortedUrls = new ArrayList<>();
             for(UrlLatency ul : currentSortedBootstrapUrls) {
                 // Make sure the URL from latency check is still in the current list
                 if(bootstrapUrls.contains(ul.url)) {
                     validSortedUrls.add(ul.url);
                 }
             }
             // 添加 bootstrapUrls 中未在排序列表里的 URL，以防万一
             for(String url : bootstrapUrls) {
                 if(!validSortedUrls.contains(url)) {
                     validSortedUrls.add(url);
                 }
             }
             urlsToTry = validSortedUrls;
             Log.d(TAG, "Using sorted bootstrap URLs based on latency.");
        } else {
            // 如果排序不可用，则随机尝试
            urlsToTry = new ArrayList<>(bootstrapUrls);
            Collections.shuffle(urlsToTry);
            Log.w(TAG, "Bootstrap URL latency sorting not available, using shuffled list.");
        }

        for (String url : urlsToTry) {
            byte[] data = attemptFetch(url, 2); // 尝试每个 URL 最多 2 次
            if (data != null && data.length > 0) {
                return data;
            }
        }
        return null; // 所有引导 URL 失败
    }

    /**
     * 从多个真实配置 URL 中获取加密的主配置数据
     */
    private byte[] fetchFromRealUrls(List<String> realConfigUrls) {
         Log.d(TAG, "Fetching from real config URLs...");
         // 可以选择对真实 URL 也进行测速和排序，或者直接随机/轮询
         // 为简单起见，这里使用随机尝试
         List<String> urlsToTry = new ArrayList<>(realConfigUrls);
         Collections.shuffle(urlsToTry);

         for (String url : urlsToTry) {
             byte[] data = attemptFetch(url, 2); // 尝试每个 URL 最多 2 次
             if (data != null && data.length > 0) {
                 return data;
             }
         }
         return null; // 所有真实 URL 失败
    }

    /**
     * 获取内置的默认加密配置
     * @return 加密的默认配置字节数组，失败返回 null
     */
    private byte[] getDefaultEncryptedConfig() {
        Log.w(TAG, "Falling back to default encrypted config.");
        byte[] mainConfigKey = null;
        byte[] encryptedDefault = null;
        try {
            if (ENCRYPTED_DEFAULT_CONFIG_BASE64 != null && !ENCRYPTED_DEFAULT_CONFIG_BASE64.startsWith("PLACEHOLDER")) {
                // 注意：这里不应该解密默认配置，fetchEncryptedConfig 期望返回加密数据
                encryptedDefault = Base64.decode(ENCRYPTED_DEFAULT_CONFIG_BASE64, Base64.DEFAULT);
                return encryptedDefault; // 直接返回加密的默认配置
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding default encrypted config", e);
        } finally {
             // 清理可能已生成的密钥
             if (mainConfigKey != null) Arrays.fill(mainConfigKey, (byte) 0);
        }
        return null;
    }


    // --- Parsing and Caching Logic ---

    /**
     * 解析 JSON 配置字符串
     * @param jsonConfig JSON 字符串
     * @return JSONObject 对象，如果解析失败返回 null
     */
    public JSONObject parseConfig(String jsonConfig) {
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(jsonConfig);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse JSON config", e);
            return null;
        }
    }

    /**
     * 检查是否应该显示对话框 (基于版本号忽略逻辑)
     * @param configObject 解析后的配置对象
     * @return true 如果应该显示，false 如果应该忽略
     */
    public boolean shouldShowDialog(JSONObject configObject) {
        if (configObject == null) {
            return false; // 无效配置不显示
        }
        // 检查总开关
        if (!configObject.optBoolean("enabled", true)) {
            Log.i(TAG, "Dialog is disabled by config.");
            return false;
        }
        // 检查版本忽略
        String configVersion = configObject.optString("version", "");
        String ignoredVersion = prefs.getString(KEY_IGNORED_VERSION, "");
        if (!configVersion.isEmpty() && configVersion.equals(ignoredVersion)) {
            Log.i(TAG, "Dialog version " + configVersion + " is ignored.");
            return false;
        }
        return true;
    }

    /**
     * 更新缓存 (内存和 SharedPreferences)
     * @param jsonConfig 最新的有效 JSON 配置字符串 (解密后的)
     */
    public void updateCache(String jsonConfig) {
        if (jsonConfig != null && !jsonConfig.isEmpty()) {
            memoryCachedConfig.set(jsonConfig);
            prefs.edit()
                 .putString(KEY_CACHED_CONFIG, jsonConfig) // 存储解密后的 JSON
                 .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                 .apply();
            Log.i(TAG, "Config cache updated.");
        }
    }

    /**
     * 获取缓存的配置 (优先内存，其次 SharedPreferences)
     * @return 缓存的 JSON 配置字符串 (解密后的)，如果无有效缓存则返回 null
     */
    public String getCachedConfig() {
        String memCache = memoryCachedConfig.get();
        if (memCache != null && !memCache.isEmpty()) {
            Log.d(TAG, "Returning config from memory cache.");
            return memCache;
        }

        long lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0);
        if (System.currentTimeMillis() - lastFetchTime < CACHE_EXPIRY_MS) {
            String diskCache = prefs.getString(KEY_CACHED_CONFIG, null);
            if (diskCache != null && !diskCache.isEmpty()) {
                Log.d(TAG, "Returning config from disk cache.");
                memoryCachedConfig.set(diskCache); // 更新内存缓存
                return diskCache;
            }
        } else {
            Log.i(TAG, "Disk cache expired.");
            // 清除过期缓存
            prefs.edit().remove(KEY_CACHED_CONFIG).remove(KEY_LAST_FETCH_TIME).apply();
        }

        return null;
    }

    /**
     * 加载缓存到内存
     */
    private void loadCache() {
        long lastFetchTime = prefs.getLong(KEY_LAST_FETCH_TIME, 0);
        if (System.currentTimeMillis() - lastFetchTime < CACHE_EXPIRY_MS) {
            String diskCache = prefs.getString(KEY_CACHED_CONFIG, null);
            if (diskCache != null && !diskCache.isEmpty()) {
                memoryCachedConfig.set(diskCache);
                Log.d(TAG, "Loaded config from disk cache to memory.");
            }
        } else {
             Log.i(TAG, "Disk cache expired on load.");
             prefs.edit().remove(KEY_CACHED_CONFIG).remove(KEY_LAST_FETCH_TIME).apply();
        }
    }

    /**
     * 异步测量并排序引导 URL 延迟
     */
    private void measureAndSortBootstrapUrlsAsync() {
        networkExecutor.submit(() -> {
            Log.d(TAG, "Starting Bootstrap URL latency measurement...");
            List<String> urlsToPing = decryptBootstrapUrls(); // 获取解密后的引导 URL
            if (urlsToPing == null || urlsToPing.isEmpty()) {
                Log.e(TAG, "Cannot measure latency, failed to get bootstrap URLs.");
                sortedBootstrapUrls.set(null);
                return;
            }

            List<Future<UrlLatency>> futures = new ArrayList<>();
            for (String url : urlsToPing) {
                futures.add(networkExecutor.submit(new PingTask(url)));
            }

            List<UrlLatency> results = new ArrayList<>();
            for (Future<UrlLatency> future : futures) {
                try {
                    UrlLatency result = future.get(5, TimeUnit.SECONDS); // 每个测速最多等待 5 秒
                    if (result != null && result.latency > 0) { // 只保留成功的测速结果
                        results.add(result);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting latency result: " + e.getMessage());
                }
            }

            if (!results.isEmpty()) {
                // 按延迟升序排序
                results.sort(Comparator.comparingLong(ul -> ul.latency));
                sortedBootstrapUrls.set(results); // 更新排序后的引导 URL 列表
                Log.i(TAG, "Bootstrap URL latency measurement complete. Sorted URLs: " + results.size());
                for(UrlLatency ul : results) {
                    Log.d(TAG, " - " + ul.url + ": " + ul.latency + "ms");
                }
            } else {
                Log.w(TAG, "Bootstrap URL latency measurement failed for all URLs.");
                sortedBootstrapUrls.set(null); // 清除旧的排序结果
            }
        });
    }

    /**
     * 用于测速的 Callable 任务
     */
    private static class PingTask implements Callable<UrlLatency> {
        private final String urlString;

        PingTask(String urlString) {
            this.urlString = urlString;
        }

        @Override
        public UrlLatency call() {
            HttpURLConnection connection = null;
            long startTime = SystemClock.elapsedRealtime();
            long latency = -1;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD"); // 使用 HEAD 请求减少数据传输
                connection.setConnectTimeout(3000); // 3 秒连接超时
                connection.setReadTimeout(3000);   // 3 秒读取超时
                connection.setUseCaches(false);
                connection.connect(); // 建立连接
                int responseCode = connection.getResponseCode(); // 获取响应码触发实际连接
                if (responseCode >= 200 && responseCode < 400) { // 认为 2xx, 3xx 都是可达的
                    latency = SystemClock.elapsedRealtime() - startTime;
                } else {
                     Log.w(TAG, "Ping failed for " + urlString + ". Response code: " + responseCode);
                }
            } catch (IOException e) {
                 Log.w(TAG, "Ping failed for " + urlString + ": " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return new UrlLatency(urlString, latency);
        }
    }

    /**
     * 记录忽略的版本号
     */
    public void ignoreVersion(String version) {
        if (version != null && !version.isEmpty()) {
            prefs.edit().putString(KEY_IGNORED_VERSION, version).apply();
            Log.i(TAG, "Version ignored: " + version);
        }
    }
}
</final_file_content>

IMPORTANT: For any future changes to this file, use the final_file_content shown above as your reference. This content reflects the current state of the file, including any auto-formatting (e.g., if you used single quotes but the formatter converted them to double quotes). Always base your SEARCH/REPLACE operations on this final version to ensure accuracy.



New problems detected after saving the file:
- [Java Error] Line 1: Could not run phased build action using connection to Gradle distribution 'https://services.gradle.org/distributions/gradle-8.11.1-all.zip'.
The specified initialization script 'C:\Users\Administrator\AppData\Roaming\Cursor\User\globalStorage\redhat.java\1.40.0\config_win\org.eclipse.osgi\58\0\.cp\gradle\init\init.gradle' does not exist.

app/src/main/java/com/hanbing/injectedpopup/config/ConfigModule.java
- [Java Error] Line 148: CONFIG_URLS cannot be resolved or is not a field
- [Java Error] Line 155: The method measureAndSortUrlsAsync() is undefined for the type ConfigModule
- [Java Error] Line 400: CONFIG_URLS cannot be resolved or is not a field<environment_details>
# VSCode Visible Files
app/src/main/java/com/hanbing/injectedpopup/config/ConfigModule.java

# VSCode Open Tabs
app/src/main/java/com/hanbing/wltc/MultiLineManager.java
../../AppData/Roaming/Cursor/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json
app/src/main/java/com/hanbing/wltc/core/util/EncodingFixer.class
.gitattributes
.editorconfig
app/src/main/java/com/hanbing/wltc/core/network/NetworkSecurityEnhancer.java
app/src/main/java/com/hanbing/wltc/core/config/ConfigFetcher.java
FixEncoding.java
app/src/main/java/com/hanbing/wltc/core/util/EncodingFixerMain.java
app/src/main/java/com/hanbing/wltc/core/util/EncodingFixer.java
app/src/main/java/com/hanbing/wltc/core/util/EncodingFixerMain.class
AndChardetHelper.class
FixQuantumUrlShield.class
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/res/values/strings.xml
app/src/main/res/layout/activity_simple_dialog.xml
app/src/main/java/com/example/onlinedialogdemo2/SimpleDialogActivity.java
test_dialog_android.html
app/src/main/res/layout/activity_custom_dialog_demo.xml
app/src/main/java/com/example/onlinedialogdemo2/MainActivity.java
.cursor/_cursor.config
LogCleanupTask.java
MainActivity.java
URLProtectionDeploymentGuide.md
StaticConfigTester.java
StaticConfigDeployer.java
StaticHostingProtection.java
config_encrypt.py
url_encrypt.py
StealthDialogTrigger.java
update_quantumshield.py
run_dialog_test.bat
test_dialog_button.bat
test_alertdialog.html
install_demo.bat
simple_demo.html
test_run.bat
install_app.bat
gradle/libs.versions.toml
app/src/main/java/com/example/onlinedialogdemo2/CustomDialogDemo.java
app/src/main/java/com/example/onlinedialogdemo2/CustomDialogDemoTest.java
app/src/androidTest/java/com/example/onlinedialogdemo2/DialogTest.java
UserGuide.md
config_decrypt.py
NanoConfigManager.java
activity_custom_dialog_demo.xml
OPTIMIZATION_SUMMARY.md
SecurityGuardian.java
SecurityInjection/src/secure/inject/extreme/CONFIG_SECURITY_README.md
SecurityInjection/src/secure/inject/extreme/ConfigSecurityTest.java
SecurityInjection/src/secure/inject/extreme/ConfigSecurityExample.java
SecurityInjection/src/secure/inject/extreme/ConfigFetcher.java
SecurityInjection/src/secure/inject/extreme/SecureConfigManager.java
SecurityInjection/src/secure/inject/extreme/FullSystemTest.java
SecurityInjection/src/secure/inject/extreme/PopupIntegrationTest.java
SecurityInjection/src/secure/inject/extreme/SimpleConfigTest.java
SecurityInjection/test_config.json
SecurityInjection/src/secure/inject/extreme/tools/ConfigEncryptionTool.java
SecurityInjection/src/secure/inject/extreme/SecurityIntegrationTest.java
SecurityInjection/src/secure/inject/extreme/SecureUrlExample.java
SecurityInjection/src/secure/inject/extreme/UltraSecurityIntegrator.java
SecurityInjection/src/secure/inject/extreme/MemoryProtector.java
SecurityInjection/src/secure/inject/extreme/NetworkSecurityEnhancer.java
SecurityInjection/src/secure/inject/extreme/SecurityInjector.java
.cursorrules
app/src/main/java/com/hanbing/wltc/core/util/tools/ConfigEncryptionTool.java
app/src/androidTest/java/com/hanbing/wltc/DialogBasicTest.java
app/src/androidTest/java/com/hanbing/wltc/SimpleTest.java
app/src/androidTest/java/com/hanbing/wltc/TestSuite.java
app/src/androidTest/java/com/hanbing/wltc/OnlineDialogLoadingTest.java
app/src/androidTest/java/com/hanbing/wltc/DialogMockUtils.java
app/src/androidTest/java/com/hanbing/wltc/FullPopupSystemTest.java
app/src/androidTest/java/com/hanbing/wltc/han.java
app/src/androidTest/java/com/hanbing/wltc/MockOnlineDialog.java
app/src/androidTest/java/com/hanbing/wltc/README.md
app/src/androidTest/java/com/hanbing/wltc/IntegrationTestRunner.java
app/src/androidTest/java/com/hanbing/wltc/SecuritySystemTest.java
app/src/androidTest/java/com/hanbing/wltc/DialogSystemTest.java
SecurityInjection/src/secure/inject/extreme/UltraSecurityManager.java
SecurityInjection/src/secure/inject/extreme/SecureUrlGenerator.java
SecurityInjection/src/secure/inject/extreme/AntiHookDetector.java
SecurityInjection/src/secure/inject/extreme/CodeObfuscator.java
SecurityInjection/src/secure/inject/extreme/AntiTamperUtils.java
SecurityInjection/build-injection.sh
SecurityInjection/build-injection.bat
SecurityInjection/src/secure/inject/extreme/SecureUrlTester.java
SecurityInjection/src/secure/inject/extreme/UrlExtractor.java
SecurityInjection/src/secure/inject/extreme/CryptoUtils.java
SecurityInjection/src/secure/inject/extreme/SecurityGuardian.java
SecurityInjection/src/secure/inject/extreme/DeviceIdentity.java
SecurityInjection/src/secure/inject/extreme/ByteArrayCombiner.java
SecurityInjection/src/secure/inject/extreme/UltraSecureUrlProvider.java
app/src/main/java/com/hanbing/wltc/LocalConfig.java
app/src/main/java/com/hanbing/wltc/LifecycleHandler.java
app/src/main/java/com/hanbing/wltc/security/QuantumUrlShield.java
app/src/main/java/com/hanbing/wltc/security/StaticConfigDeployer.java
app/src/main/java/com/hanbing/wltc/security/StaticConfigTester.java
app/src/main/java/com/hanbing/wltc/security/StaticHostingProtection.java
app/tools/android/config_decrypt.py
app/tools/android/config_encrypt.py
app/tools/android/url_encrypt.py
app/src/main/java/mutil/OnlineDialog.java
app/src/main/java/com/hanbing/wltc/TestActivity.java
app/src/main/java/mutil/C0001.java
app/src/main/java/mutil/DialogInterfaceOnClickListenerC0000.java
read_files_summary.md
app/src/main/java/com/hanbing/injectedpopup/entry/InjectionEntryPoint.java
app/src/main/java/com/hanbing/injectedpopup/dialog/DialogModule.java
app/build.gradle
gradle/wrapper/gradle-wrapper.properties
app/src/main/java/com/hanbing/injectedpopup/config/ConfigModule.java
readme.md
app/src/main/res/layout/activity_test.xml
app/src/main/java/mutil/han.java
app/src/main/AndroidManifest.xml
app/src/main/res/layout/activity_main.xml
app/src/main/java/com/hanbing/wltc/MainActivity.java
app/src/main/java/com/hanbing/wltc/han.java

# Current Time
2025/4/10 下午11:17:00 (Asia/Shanghai, UTC+8:00)

# Current Mode
ACT MODE
</environment_details>
