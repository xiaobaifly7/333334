# Java KDF 实现细节 (ConfigModule.java)

## 核心目标
在 Java 层实现一个密钥派生函数 (KDF)，用于为不同的加密目的（URL 解密、引导文件解密、主配置解密）生成不同的、难以预测的密钥。其安全性高度依赖于输入因子的选择、组合方式以及最终的 Dex 代码加固。

## KDF 算法与参数 (常量定义)

```java
    // --- KDF and Crypto Constants ---
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256"; // 标准 KDF 算法
    private static final int KDF_ITERATIONS = 10000; // 迭代次数，增加计算成本
    private static final int KDF_KEY_LENGTH_BITS = 256; // 目标密钥长度 (位)，例如 AES-256
    private static final int URL_KEY_LENGTH_BYTES = 16; // URL 解密/混淆用密钥长度 (示例)
    // ... 其他加密常量 ...
```

## KDF 输入因子

KDF 的安全性依赖于输入因子的多样性、稳定性和保密性。

### 1. 设备/环境相关信息 (`getDeviceInfoHash`)

*   **目的**: 提供随设备变化的熵。
*   **实现示例**:
    ```java
    /** 获取设备信息哈希 (示例) */
    private String getDeviceInfoHash() {
        // TODO: 实现更健壮的设备信息获取和哈希逻辑
        // 注意处理权限和 Android 版本兼容性
        byte[] hash = null;
        try {
            // 组合多个相对稳定的 Build 属性和包名
            String deviceInfo = Build.BRAND + ":" + Build.MODEL + ":" +
                              Build.MANUFACTURER + ":" + Build.PRODUCT + ":" +
                              Build.FINGERPRINT + ":" + appContext.getPackageName();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(deviceInfo.getBytes(StandardCharsets.UTF_8));
            // 返回哈希的 Base64 编码 (URL 安全)
             return Base64.encodeToString(hash, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device info hash", e);
            // 获取失败时返回一个固定的、但无意义的字符串，避免返回 null
            return "device_info_unavailable_constant_fallback";
        } finally {
             // 清理中间哈希值
             if (hash != null) Arrays.fill(hash, (byte) 0);
        }
    }
    ```
*   **注意**: 选择的设备信息应相对稳定，但系统更新可能导致 `Build.FINGERPRINT` 变化。

### 2. 嵌入的固定片段/盐值 (`getSaltPartA`, `getSaltPartB`)

*   **目的**: 增加 KDF 的复杂度，防止仅凭公开信息推导密钥。**其保密性完全依赖 Dex 加固**。
*   **实现**:
    *   **分散存储**: 将常量定义分散在不同类中。
    *   **混淆获取**: 实现简单的运行时反混淆逻辑。
    *   **常量定义 (示例)**:
        ```java
        // --- Embedded Salt/Secret Parts (依赖 Dex 加固保护) ---
        // TODO: Replace placeholders with actual obfuscated/split secret parts
        // 这些常量需要分散存储在不同地方，并依赖 Dex 加固保护
        private static final String SALT_PART_A = "OBFUSCATED_SALT_PART_A_Long_Random_String_1"; // 替换为真实混淆/加密值
        private static final byte[] SALT_PART_B = {0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F}; // 替换为真实混淆/加密值
        // ... 可能还有用于反混淆的密钥常量 ...
        ```
    *   **获取方法 (示例 - 需要实现反混淆)**:
        ```java
        /** 获取盐值片段 A (依赖 Dex 加固保护) */
        private String getSaltPartA() {
            // TODO: 实现从混淆/加密存储中获取片段的逻辑
            // 示例: return SimpleObfuscator.reveal(SALT_PART_A);
            // **必须**确保此方法返回的值是稳定的
            return SALT_PART_A; // 临时返回占位符
        }

        /** 获取盐值片段 B (依赖 Dex 加固保护) */
        private byte[] getSaltPartB() {
            // TODO: 实现从混淆/加密存储中获取片段的逻辑
            // 示例: return SimpleObfuscator.revealBytes(SALT_PART_B);
            // **必须**确保此方法返回的值是稳定的
            return Arrays.copyOf(SALT_PART_B, SALT_PART_B.length); // 返回副本
        }
        ```

### 3. 用途标识 (`purpose`)

*   **目的**: 为不同密钥（URL、引导文件、主配置）生成不同的派生结果。
*   **实现**: 在调用 KDF 时传入不同的字符串常量，如 `"URL_KEY"`, `"BOOTSTRAP_KEY"`, `"CONFIG_KEY"`。这些字符串常量也受 Dex 加固保护。

## KDF 核心实现 (`deriveKeyWithJavaKDF`)

```java
    /**
     * Java 实现的密钥派生函数 (示例 - PBKDF2) - **需要完善和保护**
     * @param purpose 密钥用途标识 (用于生成不同的盐值部分)
     * @param keyLengthBytes 期望的密钥长度（字节）
     * @return 派生的密钥字节数组，失败返回 null
     */
    private byte[] deriveKeyWithJavaKDF(String purpose, int keyLengthBytes) {
