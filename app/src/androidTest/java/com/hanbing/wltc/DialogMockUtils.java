package com.hanbing.wltc;

import android.app.Activity;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 模拟对话框相关工具类
 * 提供用于测试的辅助方法
 */
public class DialogMockUtils {
    private static final String TAG = "DialogMockUtils";

    /**
     * 创建测试配置字符串
     */
    public static String createTestConfig(String title, String message, String posBtn, String negBtn, String posColor, String negColor) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("〈title〉").append(title).append("〈/title〉");
        builder.append("〈message〉").append(message).append("〈/message〉");
        builder.append("〈posbtn〉").append(posBtn).append("〈/posbtn〉");
        builder.append("〈negbtn〉").append(negBtn).append("〈/negbtn〉");
        builder.append("〈poscolor〉").append(posColor).append("〈/poscolor〉");
        builder.append("〈negcolor〉").append(negColor).append("〈/negcolor〉");
        
        return builder.toString();
    }

    /**
     * 模拟HTTP连接的类
     */
    public static class MockURLConnection extends HttpURLConnection {
        private String responseData = "";
        private int responseCode = 200;
        private Map<String, String> requestProperties = new HashMap<>();
        
        public MockURLConnection() {
            super(null);
        }
        
        public void setResponseData(String data) {
            this.responseData = data;
        }
        
        public void setResponseCode(int code) {
            this.responseCode = code;
        }
        
        public void reset() {
            responseData = "";
            responseCode = 200;
            requestProperties.clear();
        }
        
        @Override
        public void disconnect() {
            // 不需要实现
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // 不需要实现
        }
        
        @Override
        public int getResponseCode() {
            return responseCode;
        }
        
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(responseData.getBytes());
        }
        
        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }
        
        @Override
        public String getRequestProperty(String key) {
            return requestProperties.get(key);
        }
    }
    
    /**
     * 注入模拟HTTP连接
     */
    public static void injectMockHttpConnection(final MockURLConnection mockConnection) {
        try {
            // 设置URL流处理器工厂
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
                @Override
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) {
                            return mockConnection;
                        }
                    };
                }
            });
            Log.d(TAG, "注入模拟HTTP连接成功");
        } catch (Error e) {
            Log.e(TAG, "注入模拟HTTP连接失败: " + e.getMessage());
            // 可能已经设置了工厂，忽略错误
        }
    }
} 