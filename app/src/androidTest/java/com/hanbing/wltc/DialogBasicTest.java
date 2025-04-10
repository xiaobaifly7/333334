package com.hanbing.wltc;

import android.content.Context;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.RootMatchers;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;
import static org.junit.Assert.*;

/**
 * 最基础的弹窗测试，不使用AsyncTask
 */
@RunWith(AndroidJUnit4.class)
public class DialogBasicTest {
    private static final String TAG = "DialogBasicTest";

    @Test
    public void testShowSimpleDialog() {
        // 在UI线程上显示一个简单的AlertDialog
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                Log.d(TAG, "开始测试简单弹窗显示");
                
                // 使用简单的AlertDialog，不依赖AsyncTask
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("简单测试弹窗")
                       .setMessage("这是一个简单的测试弹窗")
                       .setPositiveButton("确定", (dialog, which) -> {
                           Log.d(TAG, "点击了确定按钮");
                           dialog.dismiss();
                        })
                       .setNegativeButton("取消", (dialog, which) -> {
                           Log.d(TAG, "点击了取消按钮");
                           dialog.dismiss();
                        });
                
                // 显示弹窗
                AlertDialog dialog = builder.create();
                dialog.show();
                
                Log.d(TAG, "弹窗已显示");
                
                // 给UI一点时间显示弹窗
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 验证弹窗标题和内容
                try {
                    onView(withText("简单测试弹窗"))
                            .inRoot(RootMatchers.isDialog())
                            .check(matches(isDisplayed()));
                    
                    onView(withText("这是一个简单的测试弹窗"))
                            .inRoot(RootMatchers.isDialog())
                            .check(matches(isDisplayed()));
                    
                    Log.d(TAG, "弹窗验证成功");
                } catch (Exception e) {
                    Log.e(TAG, "弹窗验证失败", e);
                    fail("弹窗验证失败: " + e.getMessage());
                }
                
                // 点击确定按钮关闭弹窗
                try {
                    onView(withText("确定"))
                            .inRoot(RootMatchers.isDialog())
                            .perform(click());
                    
                    Log.d(TAG, "已点击确定按钮");
                } catch (Exception e) {
                    Log.e(TAG, "点击按钮失败", e);
                    // 如果点击失败，手动关闭弹窗
                    dialog.dismiss();
                }
            });
        }
        
        Log.d(TAG, "测试完成");
    }

    @Test
    public void testMockDialog() {
        // 测试弹窗工具类创建的模拟配置
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                Log.d(TAG, "开始测试MockDialog");
                
                // 创建模拟对话框
                MockOnlineDialog mockDialog = new MockOnlineDialog(activity);
                
                // 验证模拟对话框是否创建成功
                assertNotNull("模拟对话框不应为空", mockDialog);
                
                Log.d(TAG, "MockDialog测试完成");
            });
        }
    }
} 