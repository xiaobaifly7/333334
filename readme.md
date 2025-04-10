# OnlineDialogDemo2 - 弹窗注入模块优化总结

本文档总结了对 `OnlineDialogDemo2` 项目进行重构和优化的过程，旨在将其核心弹窗功能提取为一个独立的、可注入的 Dex 模块，同时满足特定的安全和兼容性需求。

## 1. 项目目标与核心需求

### 主要目标
将弹窗功能（通过网络获取配置并显示对话框）打包成一个独立的 Dex 文件，以便使用 MT 管理器等工具注入到其他 Android 应用中，并能在宿主应用中稳定、兼容地运行。

### 具体需求
1.  **服务端极简**: 服务器端（或静态托管平台）仅负责提供配置数据，逻辑尽量简单。
2.  **客户端核心化**: 注入的 Dex 模块包含大部分功能逻辑。
3.  **多线路配置获取**: 客户端能从多个预设 URL 获取配置，具备自动故障切换和延迟优化能力。
4.  **URL/IP 保护**: 最大程度保护配置 URL 不被轻易获取，并通过架构设计（如 CDN）隐藏真实服务器 IP。
5.  **配置内容加密**: 托管的配置文件内容需要加密，客户端负责解密。
6.  **客户端密钥安全**: 用于解密的密钥不能硬编码，需在客户端运行时安全派生。
7.  **高兼容性与稳定性**: 注入后的代码必须在各种 Android 环境下稳定运行，不导致宿主应用崩溃（**此项为最高优先级**）。
8.  **Dex 加固**: 最终生成的 Dex 文件需要进行强力加固，以保护其中的逻辑和数据。

## 2. 最终优化方案 (v3.1 - 兼容优先版)

为了在满足所有需求的同时将兼容性风险降至最低，最终确定采用以下方案：

*   **架构**: 引导服务 + 双重加密 + Java 层 KDF/解密 + 多线路 + CDN + Dex 加固。
*   **服务端/托管**:
    *   **引导文件**: 包含**加密后**的真实配置 URL 列表（使用 `bootstrapKey` 加密）。托管在**多个**静态 HTTPS URL (`BOOTSTRAP_URLS`)。
    *   **主配置文件**: 包含弹窗内容的 **JSON** 文件，使用**另一套密钥** (`mainConfigKey`) 通过 **AES-GCM** 加密。托管在**多个 CDN** 或静态 HTTPS URL（这些 URL 被加密存储在引导文件中）。
*   **客户端 (注入 Dex 模块)**:
    *   **URL 管理**: 在代码中存储**加密/混淆后**的 `BOOTSTRAP_URLS` 列表。运行时使用 KDF 派生的 `bootstrapUrlKey` 解密/解混淆这些 URL。
    *   **多线路获取**:
        1.  并行测速或轮询（解密后的）`BOOTSTRAP_URLS`，获取**加密的引导文件**。
        2.  使用 KDF 派生的 `bootstrapKey` 在 **Java 层**解密引导文件，得到真实配置 URL 列表。
        3.  并行测速或轮询真实配置 URL 列表，获取**加密的主配置文件**。
    *   **密钥派生 (KDF)**: 在 **Java 层**使用 `PBKDF2WithHmacSHA256` 实现 KDF。为 URL 解密、引导文件解密、主配置解密分别派生不同的密钥 (`bootstrapUrlKey`, `bootstrapKey`, `mainConfigKey`)，KDF 输入结合设备信息和代码中嵌入的混淆片段/盐值。
    *   **解密**: 在 **Java 层**使用 `javax.crypto` 实现 AES-GCM 解密主配置文件和引导文件。URL 解密使用自定义算法（如示例中的 Base64+XOR）。
    *   **弹窗逻辑**: 重构后的 `DialogModule` 负责解析解密后的 JSON 并显示 `AlertDialog`。
    *   **缓存**: 实现内存缓存和 SharedPreferences 缓存（用于版本忽略和备用）。
    *   **移除高风险代码**: **不包含** Native 代码和复杂的运行时环境检测。
*   **最终保护**: **高度依赖**对生成的 Dex 文件进行**顶级的商业加固**，以保护 URL 列表密文、KDF 逻辑、解密逻辑和嵌入的盐值片段。

## 3. 当前代码结构与路径

经过重构，核心功能代码位于 `app/src/main/java/com/hanbing/injectedpopup/` 包下：

*   `entry/InjectionEntryPoint.java`: 注入后的调用入口，协调整个流程。
*   `config/ConfigModule.java`: 负责配置获取、多线路、测速、KDF(Java 实现)、解密(Java 实现)、解析、缓存。
    *   **注意**: 此文件包含 KDF 和解密的 **Java 实现框架**，但具体算法、输入因子和占位符常量需要根据 `KDF_Implementation_Details.md` 手动填充。
*   `dialog/DialogModule.java`: 负责根据配置显示 `AlertDialog` 和处理用户交互。

**需要删除的旧代码路径 (请手动执行)**:

*   `app/src/main/java/mutil/`
*   `app/src/main/java/com/hanbing/wltc/MultiLineManager.java`
*   `app/src/main/java/com/hanbing/wltc/LocalConfig.java`
*   `app/src/main/java/com/hanbing/wltc/NanoConfigManager.java` (及相关依赖)
*   `app/src/main/java/com/hanbing/wltc/core/config/` (大部分类)
*   `app/src/main/java/com/hanbing/wltc/core/network/` (整个包)
*   `app/src/main/java/com/hanbing/wltc/core/protection/` (整个包)
*   `app/src/main/java/com/hanbing/wltc/core/security/` (整个包)
*   `app/src/main/java/com/hanbing/wltc/security/` (整个包)
*   `app/src/main/java/com/hanbing/wltc/SecurityInjector.java`
*   `app/src/main/java/com/hanbing/wltc/han.java` (如果不再被调用)

**辅助文档**:

*   `KDF_Implementation_Details.md`: 提供了 Java KDF 实现的详细建议和代码框架。

## 4. 实现历程与当前状态

1.  **初始分析**: 对原始代码库进行了全面分析，发现其包含大量复杂、冗余、硬编码且难以维护的安全机制和配置管理逻辑，以及反编译代码。
2.  **需求明确**: 用户明确了以 Dex 注入为目标，要求多线路、URL/IP 保护、配置加密，并最终将**兼容性和稳定性**置于最高优先级。
3.  **方案演进**: 讨论了多种方案，从纯 Java 简化版，到包含 Native 实现的最高安全方案，最终确定采用**方案 v3.1 (兼容优先，引导+双重加密+强加固，Java 实现)** 作为平衡点。
4.  **Java 层重构**:
    *   创建了新的包 `com.hanbing.injectedpopup`。
    *   实现了入口点 `InjectionEntryPoint`。
    *   创建并填充了 `ConfigModule` 的框架，包含多线路获取、引导文件处理、双重解密（Java 实现占位符）、KDF（Java 实现占位符）、缓存等逻辑。
    *   创建了 `DialogModule`，将旧的弹窗逻辑迁移并重构，移除了硬编码和复杂指令。
    *   更新了 `build.gradle`，解决了版本依赖问题并移除了冗余配置。
    *   创建了 `KDF_Implementation_Details.md` 文档。
5.  **当前状态**: Java 层代码框架已基本完成。

## 5. 后续步骤 (需要手动完成)

1.  **填充 `ConfigModule.java` 实现**:
    *   根据 `KDF_Implementation_Details.md` 完成 `deriveKeyWithJavaKDF` 方法中 KDF 输入因子的获取逻辑（`getDeviceInfoHash`, `getSaltPartA`, `getSaltPartB`）。
    *   完成 `decryptUrlSimpleXor` 方法（或替换为选择的 URL 解密算法）。
    *   完成 `decryptAesGcmJava` 方法（确认 AES-GCM 实现正确）。
    *   替换 `BOOTSTRAP_URLS_CIPHERTEXT` 和 `ENCRYPTED_DEFAULT_CONFIG_BASE64` 中的占位符常量。
2.  **删除旧代码**: 手动删除第 3 节中列出的所有不再需要的旧文件和目录。
3.  **服务端准备**:
    *   准备明文 JSON 配置文件和包含真实配置 URL 列表的引导文件。
    *   使用与客户端 KDF 逻辑**完全一致**的方式派生 `bootstrapKey` 和 `mainConfigKey`。
    *   使用 `bootstrapKey` 加密引导文件。
    *   使用 `mainConfigKey` 加密主配置文件和默认配置文件。
    *   使用选择的 URL 加密/混淆算法处理引导 URL。
    *   将加密后的引导文件部署到多个静态 HTTPS URL。
    *   将加密后的主配置文件部署到多个 CDN/静态 HTTPS URL。
4.  **编译与打包**:
    *   配置 `app/build.gradle` 以将 `com.hanbing.injectedpopup` 包下的代码编译成一个独立的 Dex 文件。可能需要调整 ProGuard/R8 规则以保留入口点和必要类。
5.  **代码加固**:
    *   **【极其重要】** 使用**顶级的商业 Dex 加固服务**对生成的 Dex 文件进行深度加固。这是保护客户端逻辑的关键。
6.  **注入与测试**:
    *   使用 MT 管理器等工具将**加固后的 Dex 文件**注入到目标 APK。
    *   在目标 APK 中添加对 `InjectionEntryPoint.showPopup(activity)` 的调用。
    *   在各种设备和 Android 版本上进行广泛的兼容性、稳定性和功能测试。

---
*文档生成时间: 2025/4/11*
