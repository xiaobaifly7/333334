package com.hanbing.wltc.core.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * 网络安全增强器
 * 提供SSL证书固定和安全连接功能
 */
public final class NetworkSecurityEnhancer {

    // 受信任证书 - 哈希值
    private static final String[] TRUSTED_CERT_HASHES = {
        // 证书公钥的SHA256哈希值（已混淆）
        CodeObfuscator.hideStringConstant("e5f5d4c3b2a19f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1"),
        CodeObfuscator.hideStringConstant("a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"),
        CodeObfuscator.hideStringConstant("0f1e2d3c4b5a6978675645342312f1e0d9c8b7a6958473625")
    };

    // 连接超时设置
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    
    // 验证时间间隔
    private static final long CERT_VERIFICATION_INTERVAL_MS = 86400000; // 24小时
    
    // 状态变量
    private static volatile long lastVerificationTime = 0;
    
    // 验证状态
    private static final AtomicBoolean certVerificationPassed = new AtomicBoolean(false);
    
    // 请求签名密钥 - 动态生成
    private static byte[] requestSigningKey = null;
    
    // 私有构造函数 - 防止实例化
    private NetworkSecurityEnhancer() {
        throw new UnsupportedOperationException("不允许创建网络安全增强器实例");
    }
    
    /**
     * 初始化安全增强器
     */
    public static void initialize() {
        // 生成请求签名密钥
        if (requestSigningKey == null) {
            // 使用设备指纹和时间戳派生签名密钥
            byte[] deviceFingerprint = DeviceIdentity.getFingerprintHash();
            long currentTime = System.currentTimeMillis() / CERT_VERIFICATION_INTERVAL_MS;
            
            ByteArrayCombiner combiner = new ByteArrayCombiner();
            combiner.add(deviceFingerprint);
            combiner.add(longToBytes(currentTime));
            
            requestSigningKey = CryptoUtils.sha256(combiner.toByteArray());
            combiner.clear();
        }
    }
    
    /**
     * 安全的URL连接
     * 包含证书固定和请求签名
     * 
     * @param urlString 目标URL
     * @return 响应内容
     */
    public static String secureConnect(String urlString) throws IOException {
        // 安全检查
        if (!SecurityGuardian.isSecureEnvironment()) {
            throw new SecurityException("安全环境检查失败");
        }
        
        // 确保已经初始化
        initialize();
        
        // 创建URL连接
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置超时
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        
        // 如果是HTTPS连接，应用证书固定
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            applyCertificatePinning(httpsConnection);
        }
        
        // 添加安全请求头
        addSecurityHeaders(connection);
        
        // 建立连接
        connection.connect();
        
        // 检查响应码
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP错误码: " + responseCode);
        }
        
        // 读取响应内容
        return readResponse(connection);
    }
    
    /**
     * 应用证书固定
     */
    private static void applyCertificatePinning(HttpsURLConnection connection) {
        try {
            // 检测Hook
            if (AntiHookDetector.isHookDetected()) {
                throw new SecurityException("检测到Hook工具");
            }
            
            // 创建TrustManager实现证书固定
            TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
                    public void checkClientTrusted(X509Certificate[] chain, String authType) 
                            throws CertificateException {
                        // 客户端验证 - 不实现
                    }
                    
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        // 服务器证书验证
                        if (chain == null || chain.length == 0) {
                            throw new CertificateException("服务器未提供证书");
                        }
                        
                        // 证书信任标志
                        boolean certTrusted = false;
                        
                        try {
                            // 提取证书的公钥
                            X509Certificate serverCert = chain[0];
                            byte[] publicKeyEncoded = serverCert.getPublicKey().getEncoded();
                            byte[] publicKeyHash = CryptoUtils.sha256(publicKeyEncoded);
                            String publicKeyHashHex = CryptoUtils.bytesToHex(publicKeyHash);
                            
                            // 验证公钥哈希
                            for (String trustedHashEncoded : TRUSTED_CERT_HASHES) {
                                String trustedHash = CodeObfuscator.unhideStringConstant(trustedHashEncoded);
                                if (publicKeyHashHex.equalsIgnoreCase(trustedHash)) {
                                    certTrusted = true;
                                    break;
                                }
                            }
                            
                            if (!certTrusted) {
                                throw new CertificateException("证书不受信任");
                            }
                            
                            certVerificationPassed.set(true);
                            lastVerificationTime = System.currentTimeMillis();
                            
                        } catch (Exception e) {
                            throw new CertificateException("证书验证失败", e);
                        }
                    }
                }
            };
            
            // 创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            
            // 设置SSLSocketFactory启用安全设置
            SSLSocketFactory sslSocketFactory = new SSLSocketFactoryExtension(sslContext.getSocketFactory());
            connection.setSSLSocketFactory(sslSocketFactory);
            
        } catch (Exception e) {
            throw new RuntimeException("安全配置失败", e);
        }
    }
    
    /**
     * 添加安全请求头
     */
    private static void addSecurityHeaders(HttpURLConnection connection) {
        // 设置请求方法
        try {
            connection.setRequestMethod("GET");
        } catch (Exception ignored) {
            // 忽略，可能已经设置过了
        }
        
        // 获取时间戳
        long timestamp = System.currentTimeMillis();
        
        // 生成随机噪声
        byte[] noise = new byte[8];
        new java.security.SecureRandom().nextBytes(noise);
        String noiseHex = CryptoUtils.bytesToHex(noise);
        
        // 生成签名
        String signatureData = connection.getURL().getHost() + "|" + timestamp + "|" + noiseHex;
        byte[] signature = generateRequestSignature(signatureData.getBytes());
        String signatureHex = CryptoUtils.bytesToHex(signature);
        
        // 设置安全请求头
        connection.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        connection.setRequestProperty("X-Noise", noiseHex);
        connection.setRequestProperty("X-Signature", signatureHex);
        connection.setRequestProperty("User-Agent", generateObfuscatedUserAgent());
    }
    
    /**
     * 生成混淆的User-Agent
     */
    private static String generateObfuscatedUserAgent() {
        // 生成随机化但合理的User-Agent
        return "Mozilla/5.0 (Linux; Android " + getObfuscatedAndroidVersion() + 
               "; " + getObfuscatedDeviceInfo() + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36";
    }
    
    /**
     * 获取混淆的Android版本
     */
    private static String getObfuscatedAndroidVersion() {
        // 基于实际版本轻微混淆
        try {
            String actual = System.getProperty("ro.build.version.release", "10");
            int version = Integer.parseInt(actual.split("\\.")[0]);
            // 在真实版本上下浮动一个版本
            return String.valueOf(Math.max(8, Math.min(13, version + (int)(Math.random() * 3) - 1)));
        } catch (Exception e) {
            return "10";
        }
    }
    
    /**
     * 获取混淆的设备信息
     */
    private static String getObfuscatedDeviceInfo() {
        // 常见厂商和型号列表
        String[] manufacturers = {"Samsung", "Xiaomi", "Huawei", "OPPO", "Vivo"};
        String[] models = {"SM-G9750", "Redmi Note 8", "P30 Pro", "Reno5", "X60"};
        
        // 根据设备hash确定选择的厂商和型号
        byte[] fingerprint = DeviceIdentity.getFingerprintHash();
        int mIndex = Math.abs(fingerprint[0]) % manufacturers.length;
        int modelIndex = Math.abs(fingerprint[1]) % models.length;
        
        return manufacturers[mIndex] + " " + models[modelIndex];
    }
    
    /**
     * 生成请求签名
     */
    private static byte[] generateRequestSignature(byte[] data) {
        if (requestSigningKey == null) {
            initialize();
        }
        
        ByteArrayCombiner combiner = new ByteArrayCombiner();
        combiner.add(data);
        combiner.add(requestSigningKey);
        
        byte[] signatureData = combiner.toByteArray();
        try {
            return CryptoUtils.sha256(signatureData);
        } finally {
            combiner.clear();
        }
    }
    
    /**
     * 读取响应内容
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                // 转换为UTF-8字符串并追加
                String chunk = new String(buffer, 0, bytesRead, "UTF-8");
                response.append(chunk);
            }
        }
        
        // 返回响应内容
        String responseString = response.toString();
        if (SecurityGuardian.quickSecurityCheck() && !AntiHookDetector.isHookDetected()) {
            return responseString;
        } else {
            // 如果安全检查失败，返回伪造响应
            return generateFakeResponse();
        }
    }
    
    /**
     * 生成伪造响应
     */
    private static String generateFakeResponse() {
        // 返回一个合理的JSON错误
        return "{\"status\":\"error\",\"code\":403,\"message\":\"Unauthorized access detected\"}";
    }
    
    /**
     * 自定义SSLSocketFactory加强安全配置
     */
    private static class SSLSocketFactoryExtension extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        
        public SSLSocketFactoryExtension(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        @Override
        public SSLSocket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(socket, host, port, autoClose);
            configureTLS(sslSocket);
            return sslSocket;
        }
        
        @Override
        public SSLSocket createSocket(String host, int port) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
            configureTLS(sslSocket);
            return sslSocket;
        }
        
        @Override
        public SSLSocket createSocket(String host, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port, localAddress, localPort);
            configureTLS(sslSocket);
            return sslSocket;
        }
        
        @Override
        public SSLSocket createSocket(java.net.InetAddress host, int port) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(host, port);
            configureTLS(sslSocket);
            return sslSocket;
        }
        
        @Override
        public SSLSocket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            SSLSocket sslSocket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
            configureTLS(sslSocket);
            return sslSocket;
        }
        
        /**
         * 配置TLS设置
         */
        private void configureTLS(SSLSocket sslSocket) {
            // 只启用安全TLS版本
            sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
            
            // 可以进一步加强密码套件
        }
    }
    
    /**
     * 将long类型转为byte数组
     */
    private static byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(value & 0xFF);
            value >>= 8;
        }
        return result;
    }
}