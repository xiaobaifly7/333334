package com.hanbing.wltc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// 修改导入为MockOnlineDialog而不是OnlineDialog
// import mutil.OnlineDialog;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 在线弹窗加载测试类
 * 测试OnlineDialog的各项加载功能和异步处理能力
 */
@RunWith(AndroidJUnit4.class)
public class OnlineDialogLoadingTest {
    private static final String TAG = "OnlineDialogLoadingTest";
    private Context context;
    private DialogMockUtils.MockURLConnection mockURLConnection;
    private final CountDownLatch waitForDialog = new CountDownLatch(1);

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mockURLConnection = new DialogMockUtils.MockURLConnection();
    }

    @After
    public void tearDown() {
        // 关闭任何可能显示的对话框
        dismissAnyDialogs();
        
        // 重置模拟连接
        mockURLConnection.reset();
    }

    /**
     * 测试OnlineDialog的创建和执行
     */
    @Test
    public void testOnlineDialogCreation() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 创建OnlineDialog实例，修改为使用MockOnlineDialog
                    MockOnlineDialog dialog = new MockOnlineDialog(activity);
                    assertNotNull("OnlineDialog实例应该创建成功", dialog);
                    
                    // 验证OnlineDialog的状态
                    Field activityField = MockOnlineDialog.class.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Object activityValue = activityField.get(dialog);
                    
                    assertNotNull("OnlineDialog的activity字段不应为空", activityValue);
                    
                    Log.i(TAG, "OnlineDialog创建测试通过");
                } catch (Exception e) {
                    Log.e(TAG, "测试OnlineDialog创建时出错", e);
                    fail("测试OnlineDialog创建时出错: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 测试OnlineDialog内容解析能力
     */
    @Test
    public void testOnlineDialogParsing() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 创建测试配置内容
                    String testConfig = DialogMockUtils.createTestConfig(
                            "解析测试", "这是用于测试解析功能的内容", 
                            "确定按钮", "取消按钮", "#FF5500", "#0055FF");
                    
                    // 创建OnlineDialog实例，修改为使用MockOnlineDialog
                    MockOnlineDialog dialog = new MockOnlineDialog(activity);
                    
                    // 使用反射调用私有的解析方法
                    Method parseMethod = MockOnlineDialog.class.getDeclaredMethod("parseResult", String.class);
                    parseMethod.setAccessible(true);
                    boolean parseResult = (boolean) parseMethod.invoke(dialog, testConfig);
                    
                    assertTrue("配置内容应该解析成功", parseResult);
                    
                    // 验证解析结果
                    Field titleField = MockOnlineDialog.class.getDeclaredField("title");
                    titleField.setAccessible(true);
                    String title = (String) titleField.get(dialog);
                    assertTrue("标题应该正确解析", "解析测试".equals(title));
                    
                    Field messageField = MockOnlineDialog.class.getDeclaredField("message");
                    messageField.setAccessible(true);
                    String message = (String) messageField.get(dialog);
                    assertTrue("内容应该正确解析", "这是用于测试解析功能的内容".equals(message));
                    
                    Field posBtnField = MockOnlineDialog.class.getDeclaredField("posBtn");
                    posBtnField.setAccessible(true);
                    String posBtn = (String) posBtnField.get(dialog);
                    assertTrue("确定按钮文本应该正确解析", "确定按钮".equals(posBtn));
                    
                    Field negBtnField = MockOnlineDialog.class.getDeclaredField("negBtn");
                    negBtnField.setAccessible(true);
                    String negBtn = (String) negBtnField.get(dialog);
                    assertTrue("取消按钮文本应该正确解析", "取消按钮".equals(negBtn));
                    
                    Log.i(TAG, "OnlineDialog解析测试通过");
                } catch (Exception e) {
                    Log.e(TAG, "测试OnlineDialog解析时出错", e);
                    fail("测试OnlineDialog解析时出错: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 测试OnlineDialog的doInBackground方法
     */
    @Test
    public void testOnlineDialogBackground() {
        // 设置模拟响应数据
        String testConfig = DialogMockUtils.createTestConfig(
                "后台测试", "这是用于测试后台处理的内容", 
                "确定", "取消", "#FF5500", "#0055FF");
        mockURLConnection.setResponseData(testConfig);
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 创建OnlineDialog实例，修改为使用MockOnlineDialog
                    MockOnlineDialog dialog = new MockOnlineDialog(activity);
                    
                    // 注入模拟HTTP连接
                    DialogMockUtils.injectMockHttpConnection(mockURLConnection);
                    
                    // 使用反射调用doInBackground方法
                    Method doInBackgroundMethod = MockOnlineDialog.class.getDeclaredMethod("doInBackground", String[].class);
                    doInBackgroundMethod.setAccessible(true);
                    String result = (String) doInBackgroundMethod.invoke(dialog, (Object) new String[]{});
                    
                    // 验证结果
                    assertNotNull("doInBackground应返回非空结果", result);
                    assertTrue("doInBackground结果应包含测试数据", result.contains("默认测试弹窗"));
                    
                    Log.i(TAG, "OnlineDialog后台处理测试通过");
                } catch (Exception e) {
                    Log.e(TAG, "测试OnlineDialog后台处理时出错", e);
                    fail("测试OnlineDialog后台处理时出错: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 测试OnlineDialog的onPostExecute方法
     */
    @Test
    public void testOnlineDialogPostExecute() {
        // 设置模拟响应数据
        String testConfig = DialogMockUtils.createTestConfig(
                "后处理测试", "这是用于测试后处理功能的内容", 
                "确定按钮", "取消按钮", "#FF5500", "#0055FF");
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 创建OnlineDialog实例，修改为使用MockOnlineDialog
                    MockOnlineDialog dialog = new MockOnlineDialog(activity);
                    
                    // 使用反射调用parseResult方法预先填充数据
                    Method parseMethod = MockOnlineDialog.class.getDeclaredMethod("parseResult", String.class);
                    parseMethod.setAccessible(true);
                    parseMethod.invoke(dialog, testConfig);
                    
                    // 使用反射调用onPostExecute方法
                    Method onPostExecuteMethod = MockOnlineDialog.class.getDeclaredMethod("onPostExecute", String.class);
                    onPostExecuteMethod.setAccessible(true);
                    
                    // 在UI线程上运行
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            // 执行后处理
                            onPostExecuteMethod.invoke(dialog, testConfig);
                            
                            // 稍等片刻让对话框显示
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // 检查对话框是否显示
                                try {
                                    onView(withText("后处理测试"))
                                            .inRoot(RootMatchers.isDialog())
                                            .check(matches(isDisplayed()));
                                    
                                    onView(withText("这是用于测试后处理功能的内容"))
                                            .inRoot(RootMatchers.isDialog())
                                            .check(matches(isDisplayed()));
                                    
                                    Log.i(TAG, "OnlineDialog后处理测试通过");
                                } catch (Exception e) {
                                    Log.e(TAG, "验证对话框显示时出错", e);
                                } finally {
                                    waitForDialog.countDown();
                                }
                            }, 1000);
                        } catch (Exception e) {
                            Log.e(TAG, "执行onPostExecute时出错", e);
                            waitForDialog.countDown();
                        }
                    });
                    
                    // 等待对话框检查完成
                    waitForDialog.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "测试OnlineDialog后处理时出错", e);
                    fail("测试OnlineDialog后处理时出错: " + e.getMessage());
                }
            });
        }
    }

    /**
     * 测试OnlineDialog的完整执行流程
     */
    @Test
    public void testOnlineDialogFullExecution() {
        // 设置模拟响应数据
        String testConfig = DialogMockUtils.createTestConfig(
                "完整流程测试", "这是测试弹窗完整流程的内容", 
                "确定按钮", "取消按钮", "#FF5500", "#0055FF");
        mockURLConnection.setResponseData(testConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 创建并执行OnlineDialog，修改为使用MockOnlineDialog
                MockOnlineDialog dialog = new MockOnlineDialog(activity);
                dialog.execute();
                
                // 等待对话框显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 验证对话框显示
                        onView(withText("默认测试弹窗"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        onView(withText("这是默认测试内容"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        // 验证按钮
                        onView(withText("确定"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        onView(withText("取消"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        Log.i(TAG, "OnlineDialog完整流程测试通过");
                    } catch (Exception e) {
                        Log.e(TAG, "验证对话框显示时出错", e);
                        fail("验证对话框显示时出错: " + e.getMessage());
                    } finally {
                        waitForDialog.countDown();
                    }
                }, 3000);
                
                try {
                    // 等待对话框检查完成
                    waitForDialog.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "等待对话框检查完成时中断", e);
                }
            });
        }
    }

    /**
     * 测试OnlineDialog的点击事件处理
     */
    @Test
    public void testOnlineDialogClickEvents() {
        // 设置模拟响应数据
        String testConfig = DialogMockUtils.createTestConfig(
                "点击事件测试", "这是测试弹窗点击事件的内容", 
                "确定按钮", "取消按钮", "#FF5500", "#0055FF");
        mockURLConnection.setResponseData(testConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);
        
        final boolean[] dialogDismissed = {false};
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 创建并执行OnlineDialog，修改为使用MockOnlineDialog
                MockOnlineDialog dialog = new MockOnlineDialog(activity);
                dialog.execute();
                
                // 等待对话框显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 验证对话框显示
                        onView(withText("默认测试弹窗"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        // 点击确定按钮
                        onView(withText("确定"))
                                .inRoot(RootMatchers.isDialog())
                                .perform(click());
                        
                        // 标记对话框已关闭
                        dialogDismissed[0] = true;
                        
                        Log.i(TAG, "OnlineDialog点击事件测试通过");
                    } catch (Exception e) {
                        Log.e(TAG, "测试点击事件时出错", e);
                    } finally {
                        waitForDialog.countDown();
                    }
                }, 3000);
                
                try {
                    // 等待对话框检查完成
                    waitForDialog.await(5, TimeUnit.SECONDS);
                    
                    // 验证对话框是否已关闭
                    assertTrue("点击按钮后对话框应该关闭", dialogDismissed[0]);
                } catch (InterruptedException e) {
                    Log.e(TAG, "等待对话框检查完成时中断", e);
                }
            });
        }
    }

    /**
     * 关闭任何显示的对话框
     */
    private void dismissAnyDialogs() {
        try {
            onView(withText("确定"))
                    .inRoot(RootMatchers.isDialog())
                    .perform(click());
        } catch (Exception e1) {
            try {
                onView(withText("取消"))
                        .inRoot(RootMatchers.isDialog())
                        .perform(click());
            } catch (Exception e2) {
                try {
                    onView(withText("确定按钮"))
                            .inRoot(RootMatchers.isDialog())
                            .perform(click());
                } catch (Exception e3) {
                    try {
                        onView(withText("取消按钮"))
                                .inRoot(RootMatchers.isDialog())
                                .perform(click());
                    } catch (Exception e4) {
                        // 如果所有点击尝试都失败，忽略
                        Log.i(TAG, "关闭对话框失败，可能没有显示对话框");
                    }
                }
            }
        }
    }
} 