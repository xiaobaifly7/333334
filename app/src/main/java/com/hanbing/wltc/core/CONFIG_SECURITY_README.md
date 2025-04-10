# 配置安全系统使用说明

## 概述

配置安全系统是一个用于保护应用程序配置内容的完整解决方案。它通过多层加密、设备绑定和安全存储机制，确保敏感配置信息（如功能开关、服务端点URL、业务参数等）不会被轻易提取或篡改。该系统与已有的URL安全保护机制协同工作，形成端到端的配置安全保障。

## 系统架构

配置安全系统由以下核心组件构成：

1. **ConfigEncryptionTool**: 配置加密工具，用于离线预处理配置文件
2. **SecureConfigManager**: 安全配置管理器，处理配置解密和验证
3. **ConfigFetcher**: 配置获取器，负责从安全URL获取配置并管理缓存
4. **MemoryProtector**: 内存保护组件，确保解密后的配置在内存中安全存储
5. **DeviceIdentity**: 设备身份组件，生成用于配置绑定的设备指纹
6. **ConfigSecurityExample**: 使用示例类，展示如何集成和使用该系统
7. **ConfigSecurityTest**: 测试类，验证系统功能完整性

## 安全特性

- **设备绑定**: 配置与特定设备或设备组绑定，防止跨设备提取
- **多层加密**: 使用AES-GCM加密算法和设备衍生密钥
- **HMAC验证**: 确保配置数据完整性和真实性
- **内存保护**: 解密后的配置存储在受保护内存区域
- **过期机制**: 配置自动过期，强制定期更新
- **随机文件名**: 加密后的配置使用随机文件名存储，增加攻击难度
- **安全清除**: 配置使用完毕后安全清除内存

## 使用流程

### 1. 服务端配置准备

1. 创建配置文件（JSON格式）
2. 使用ConfigEncryptionTool预处理加密配置文件
3. 将加密后的配置文件部署到静态资源服务器或CDN

```shell
# 命令行用法
java -jar config-encryption-tool.jar <配置文件路径> <设备组标识> <输出目录>

# 示例
java -jar config-encryption-tool.jar config.json group_alpha ./configs
```

### 2. 客户端集成

1. 初始化安全组件

```java
// 在应用启动时初始化
UltraSecurityManager.getInstance().enableMemoryProtection();
UltraSecurityManager.getInstance().enableNetworkSecurity();
SecureConfigManager.initialize();

// 初始化配置获取器
ConfigFetcher fetcher = ConfigFetcher.getInstance();
fetcher.setDefaultConfig("{\"version\":\"1.0\"}"); // 设置默认配置
```

2. 设置配置更新回调（可选）

```java
fetcher.setUpdateCallback(new ConfigFetcher.ConfigUpdateCallback() {
    @Override
    public void onConfigUpdated(String config) {
        // 配置更新成功的处理逻辑
    }
    
    @Override
    public void onConfigUpdateFailed(Exception e) {
        // 配置更新失败的处理逻辑
    }
});
```

3. 获取配置内容

```java
// 获取整个配置
String config = ConfigFetcher.getInstance().getConfig();

// 获取特定功能开关
boolean isFeatureEnabled = ConfigSecurityExample.isFeatureEnabled("featureName", false);

// 获取特定参数
String paramValue = ConfigSecurityExample.getConfigParam("settings.timeout", "30");
```

4. 强制更新配置（可选）

```java
ConfigFetcher.getInstance().forceUpdate();
```

## 配置格式

建议使用JSON格式的配置文件，示例如下：

```json
{
  "version": "1.0",
  "lastUpdated": "2023-06-01T12:00:00Z",
  "features": {
    "featureA": true,
    "featureB": false,
    "premiumFeature": false
  },
  "settings": {
    "timeout": 30,
    "retries": 3,
    "cacheTime": 86400
  },
  "endpoints": {
    "api": "https://api.example.com/v1",
    "analytics": "https://analytics.example.com"
  }
}
```

## 安全最佳实践

1. **定期更新配置加密密钥**: 每个版本更新时更改设备组密钥
2. **使用复杂配置结构**: 避免使用直观的键名，考虑使用代码或混淆技术
3. **实现版本检查**: 确保旧版本客户端无法使用新配置
4. **设置合理过期时间**: 权衡安全性和用户体验
5. **监控异常访问**: 服务端记录和分析配置获取模式，检测异常行为
6. **配置分层**: 将不同敏感级别的配置分开处理

## 性能考虑

- 配置解密和验证是CPU密集型操作，建议在后台线程中进行
- 解密后的配置会缓存在内存中，避免频繁解密操作
- 配置更新间隔默认为6小时，可根据需要调整

## 常见问题排查

1. **配置解密失败**
   - 检查设备ID是否正确
   - 确认配置文件路径是否正确
   - 验证配置文件格式和加密算法版本

2. **无法获取配置**
   - 检查网络连接
   - 验证安全URL是否正确
   - 确认配置文件是否存在于服务器

3. **配置验证失败**
   - 检查HMAC密钥是否一致
   - 确认配置内容是否被篡改
   - 验证配置是否已过期

## 测试与验证

使用`ConfigSecurityTest`类进行系统功能测试：

```java
// 运行完整测试
java secure.inject.extreme.ConfigSecurityTest
```

测试内容包括：
- 配置加密过程
- 配置解密和验证
- 内存保护功能

## 集成示例

参考`ConfigSecurityExample`类获取完整集成示例代码。