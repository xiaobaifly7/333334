package com.hanbing.wltc;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * 最简单的测试类，测试弹窗功能的基础组件
 */
@RunWith(AndroidJUnit4.class)
public class SimpleTest {

    @Test
    public void useAppContext() {
        // 上下文测试
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull("上下文不应为空", appContext);
        assertTrue("包名应包含hanbing.wltc", 
                appContext.getPackageName().contains("hanbing.wltc"));
    }

    @Test
    public void testDialogMock() {
        // 测试对话框模拟工具
        String testConfig = DialogMockUtils.createTestConfig(
                "测试标题", "测试内容", "确定", "取消", "#FF0000", "#00FF00");
        assertNotNull("创建的测试配置不应为空", testConfig);
        assertTrue("配置应包含标题", testConfig.contains("测试标题"));
        assertTrue("配置应包含内容", testConfig.contains("测试内容"));
    }

    @Test
    public void testMockURLConnection() {
        // 测试模拟HTTP连接
        DialogMockUtils.MockURLConnection connection = new DialogMockUtils.MockURLConnection();
        assertNotNull("模拟连接不应为空", connection);
        
        // 设置响应数据
        String testResponse = "测试响应";
        connection.setResponseData(testResponse);
        
        // 检查响应码
        assertEquals("默认响应码应为200", 200, connection.getResponseCode());
        
        // 修改响应码并验证
        connection.setResponseCode(404);
        assertEquals("响应码应被修改为404", 404, connection.getResponseCode());
    }
} 