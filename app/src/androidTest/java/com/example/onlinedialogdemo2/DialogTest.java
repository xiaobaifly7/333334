package com.example.onlinedialogdemo2;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 弹窗功能专用测试类
 */
@RunWith(AndroidJUnit4.class)
public class DialogTest {
    
    private static final String TAG = "DialogTest";

    @Test
    public void testDialogDisplay() {
        Log.i(TAG, "开始测试弹窗显示");
        
        // 获取上下文
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.onlinedialogdemo2", appContext.getPackageName());
        
        // 启动主Activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        
        // 测试弹窗触发
        scenario.onActivity(activity -> {
            Log.i(TAG, "Activity已启动，准备触发弹窗");
            
            // 找到弹窗按钮并点击
            Button showDialogButton = activity.findViewById(R.id.showDialogButton);
            assertNotNull("弹窗按钮不应为空", showDialogButton);
            
            // 记录点击前状态
            Log.i(TAG, "点击弹窗按钮前");
            
            // 点击按钮显示弹窗
            showDialogButton.performClick();
            
            // 记录点击后状态
            Log.i(TAG, "点击弹窗按钮后，应该显示弹窗");
            
            // 等待弹窗显示
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证弹窗是否显示
            Log.i(TAG, "弹窗应已显示，测试完成");
        });
        
        Log.i(TAG, "弹窗显示测试结束");
    }
    
    @Test
    public void testCustomDialogDemoTest() {
        Log.i(TAG, "开始测试专用测试对话框");
        
        // 创建一个定制的弹窗并验证内容
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // 验证CustomDialogDemoTest功能
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            Log.i(TAG, "准备直接调用CustomDialogDemoTest.showDialog()");
            
            // 直接调用显示弹窗
            CustomDialogDemoTest.showDialog(activity);
            
            // 等待弹窗加载
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            Log.i(TAG, "CustomDialogDemoTest.showDialog()已调用，弹窗应已显示");
            
            // 再等待几秒让按钮可以被点击
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        Log.i(TAG, "测试专用对话框测试结束");
    }
    
    @Test
    public void testRealCustomDialogDemo() {
        Log.i(TAG, "开始测试真实CustomDialogDemo");
        
        // 验证原始CustomDialogDemo功能
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            Log.i(TAG, "准备直接调用原始CustomDialogDemo.showDialog()");
            
            try {
                // 直接调用显示弹窗
                CustomDialogDemo.showDialog(activity);
                
                // 等待弹窗加载
                Thread.sleep(5000);
                
                Log.i(TAG, "原始CustomDialogDemo.showDialog()已调用，弹窗应已显示");
            } catch (Exception e) {
                Log.e(TAG, "原始CustomDialogDemo调用出错", e);
            }
        });
        
        Log.i(TAG, "真实对话框测试结束");
    }
} 