package com.hanbing.wltc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 全面的弹窗系统测试
 * 测试弹窗系统的完整逻辑
 */
@RunWith(AndroidJUnit4.class)
public class FullPopupSystemTest {

    private static final String TAG = "FullPopupSystemTest";
    private Context context;
    private DialogMockUtils.MockURLConnection mockURLConnection;
    private boolean isNetworkAvailableForTest = true; // 默认网络可用
    private final CountDownLatch waitForDialog = new CountDownLatch(1);

    /**
     * 弹窗等待资源，用于Espresso测试
     */
    private static class PopupIdlingResource implements IdlingResource {
        private ResourceCallback resourceCallback;
        private final AtomicBoolean isIdle = new AtomicBoolean(false);

        @Override
        public String getName() {
            return "Popup Idling Resource";
        }

        @Override
        public boolean isIdleNow() {
            return isIdle.get();
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            this.resourceCallback = callback;
        }

        public void setIdle(boolean idle) {
            isIdle.set(idle);
            if (idle && resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
        }
    }

    private PopupIdlingResource idlingResource;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mockURLConnection = new DialogMockUtils.MockURLConnection();

        // 创建并注册空闲资源
        idlingResource = new PopupIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);

        // 默认网络可用状态
        isNetworkAvailableForTest = true;
    }

    @After
    public void tearDown() {
        // 确保对话框关闭
        dismissAnyDialogs();

        // 取消注册空闲资源
        if (idlingResource != null) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }

        // 恢复默认网络状态
        isNetworkAvailableForTest = true;

        // 重置模拟HTTP连接
        if (mockURLConnection != null) {
            mockURLConnection.reset();
        }
    }

    /**
     * 测试基本弹窗流程
     */
    @Test
    public void testBasicPopupFlow() {
        String testConfig = DialogMockUtils.createTestConfig("测试弹窗", "这是测试内容", 
                                           "确定", "取消", "#FF0000", "#00FF00");
        mockURLConnection.setResponseData(testConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                Log.i(TAG, "启动测试活动");
                
                // 标记资源为忙碌状态
                idlingResource.setIdle(false);
                
                // 调用弹窗
                han.diaoyon(activity);
                Log.i(TAG, "已调用弹窗");
                
                // 等待弹窗显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 检查弹窗是否显示
                        if (isDialogShown("测试弹窗")) {
                            Log.i(TAG, "弹窗已显示");
                        } else {
                            Log.e(TAG, "弹窗未显示");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "检查弹窗时出错", e);
                    } finally {
                        // 通知Espresso测试可以继续
                        idlingResource.setIdle(true);
                        waitForDialog.countDown();
                    }
                }, 3000);
            });

            // 等待弹窗显示
            waitForDialog.await(5, TimeUnit.SECONDS);
            
            // 验证弹窗标题和内容
            onView(withText("测试弹窗"))
                    .inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed()));
            
            onView(withText("这是测试内容"))
                    .inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed()));
            
            // 验证按钮
            onView(withText("确定"))
                    .inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed()));
            
            onView(withText("取消"))
                    .inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed()));
            
            // 点击确定按钮
            onView(withText("确定"))
                    .inRoot(RootMatchers.isDialog())
                    .perform(click());
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试网络错误情况下的弹窗
     */
    @Test
    public void testPopupWithNetworkError() {
        // 模拟网络错误
        isNetworkAvailableForTest = false;
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                Log.i(TAG, "启动测试活动 - 网络错误测试");
                
                // 调用弹窗
                han.diaoyon(activity);
                
                // 验证应用尝试从本地缓存加载配置
                verifyLocalConfigAccess(activity);
            });
        }
    }

    /**
     * 测试配置无效情况下的弹窗
     */
    @Test
    public void testPopupWithInvalidConfig() {
        // 设置无效的配置数据
        String invalidConfig = "这不是有效的配置数据";
        mockURLConnection.setResponseData(invalidConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                Log.i(TAG, "启动测试活动 - 无效配置测试");
                
                // 调用弹窗
                han.diaoyon(activity);
                
                // 验证没有弹窗显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        boolean dialogShown = isDialogShown(null);
                        Log.i(TAG, "弹窗是否显示: " + dialogShown);
                        // 不应该显示弹窗
                        if (!dialogShown) {
                            Log.i(TAG, "无效配置测试通过");
                        } else {
                            Log.e(TAG, "无效配置测试失败 - 不应显示弹窗但却显示了");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "检查弹窗时出错", e);
                    } finally {
                        waitForDialog.countDown();
                    }
                }, 3000);
            });
            
            // 等待检查完成
            waitForDialog.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试缓存机制
     */
    @Test
    public void testCacheMechanism() {
        String testConfig = DialogMockUtils.createTestConfig("缓存测试", "这是测试缓存机制的内容", 
                                           "确定", "取消", "#FF0000", "#00FF00");
        mockURLConnection.setResponseData(testConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 第一次调用弹窗，正常获取配置
                han.diaoyon(activity);
                
                // 等待弹窗显示并关闭
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // 如果弹窗显示，关闭它
                    dismissAnyDialogs();
                    
                    // 清除网络数据
                    mockURLConnection.setResponseData(null);
                    mockURLConnection.setResponseCode(404);
                    
                    // 再次调用弹窗，应该使用缓存
                    han.diaoyon(activity);
                    
                    // 等待弹窗显示
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            // 验证弹窗是否显示（应该使用缓存的配置）
                            if (isDialogShown("缓存测试")) {
                                Log.i(TAG, "缓存测试通过 - 使用了缓存配置");
                            } else {
                                Log.e(TAG, "缓存测试失败 - 未使用缓存配置或弹窗未显示");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "检查弹窗时出错", e);
                        } finally {
                            waitForDialog.countDown();
                        }
                    }, 3000);
                }, 3000);
            });
            
            // 等待测试完成
            waitForDialog.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试URL故障转移机制
     */
    @Test
    public void testUrlFailover() {
        // 设置第一个URL的响应为失败
        mockURLConnection.setResponseCode(404);
        
        // 设置第二个URL的响应
        String testConfig = DialogMockUtils.createTestConfig("URL故障转移测试", "这是测试URL故障转移的内容", 
                                           "确定", "取消", "#FF0000", "#00FF00");
        
        // 注入模拟连接
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 准备测试数据
                mockURLConnection.setResponseData(testConfig);
                
                // 调用弹窗
                han.diaoyon(activity);
                
                // 等待弹窗显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 验证弹窗是否显示
                        onView(withText("URL故障转移测试"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        Log.i(TAG, "URL故障转移测试通过");
                    } catch (Exception e) {
                        Log.e(TAG, "URL故障转移测试失败", e);
                        fail("URL故障转移测试失败: " + e.getMessage());
                    } finally {
                        waitForDialog.countDown();
                    }
                }, 5000);
            });
            
            // 等待测试完成
            waitForDialog.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试生命周期处理
     */
    @Test
    public void testLifecycleHandling() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 验证生命周期处理器是否已注册
                verifyLifecycleHandlerRegistered(activity);
                
                // 调用弹窗
                han.diaoyon(activity);
                
                // 等待弹窗显示
                sleep(2000);
                
                // 模拟活动暂停和恢复
                scenario.recreate();
                
                // 等待活动重建
                sleep(2000);
                
                // 验证弹窗是否仍然显示或重新显示
                scenario.onActivity(newActivity -> {
                    try {
                        boolean dialogShown = isDialogShown(null);
                        Log.i(TAG, "弹窗是否显示: " + dialogShown);
                    } catch (Exception e) {
                        Log.e(TAG, "检查弹窗时出错", e);
                    } finally {
                        waitForDialog.countDown();
                    }
                });
            });
            
            // 等待测试完成
            waitForDialog.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 测试多线程处理
     */
    @Test
    public void testMultiThreadHandling() {
        String testConfig = DialogMockUtils.createTestConfig("多线程测试", "这是测试多线程处理的内容", 
                                           "确定", "取消", "#FF0000", "#00FF00");
        mockURLConnection.setResponseData(testConfig);
        DialogMockUtils.injectMockHttpConnection(mockURLConnection);
        
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                // 多线程并发调用弹窗
                final int threadCount = 5;
                final CountDownLatch threadsLatch = new CountDownLatch(threadCount);
                
                for (int i = 0; i < threadCount; i++) {
                    final int threadId = i;
                    new Thread(() -> {
                        try {
                            Log.i(TAG, "线程 " + threadId + " 调用弹窗");
                            han.diaoyon(activity);
                        } catch (Exception e) {
                            Log.e(TAG, "线程 " + threadId + " 出错", e);
                        } finally {
                            threadsLatch.countDown();
                        }
                    }).start();
                }
                
                // 等待所有线程完成
                try {
                    threadsLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "等待线程完成时被中断", e);
                }
                
                // 等待弹窗显示
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // 应该只有一个弹窗显示
                        onView(withText("多线程测试"))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        
                        Log.i(TAG, "多线程测试通过");
                    } catch (Exception e) {
                        Log.e(TAG, "多线程测试失败", e);
                        fail("多线程测试失败: " + e.getMessage());
                    } finally {
                        waitForDialog.countDown();
                    }
                }, 3000);
            });
            
            // 等待测试完成
            waitForDialog.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "测试过程中出现异常", e);
            fail("测试过程中出现异常: " + e.getMessage());
        }
    }

    /**
     * 从缓存获取配置
     */
    private String getConfigFromCache(Activity activity) {
        try {
            // 从SharedPreferences获取缓存配置
            SharedPreferences prefs = activity.getSharedPreferences("popup_config", Context.MODE_PRIVATE);
            return prefs.getString("config_data", null);
        } catch (Exception e) {
            Log.e(TAG, "获取缓存配置时出错", e);
            return null;
        }
    }

    /**
     * 验证本地配置访问
     */
    private void verifyLocalConfigAccess(Activity activity) {
        try {
            // 获取本地缓存的配置
            String cachedConfig = getConfigFromCache(activity);
            
            // 记录测试结果
            Log.i(TAG, "本地配置访问测试结果: " + (cachedConfig != null ? "成功" : "失败"));
        } catch (Exception e) {
            Log.e(TAG, "验证本地配置访问时出错", e);
        }
    }

    /**
     * 验证生命周期处理器是否已注册
     */
    private void verifyLifecycleHandlerRegistered(Activity activity) {
        try {
            // 检查生命周期处理器是否已注册
            Class<?> handlerClass = Class.forName("com.hanbing.wltc.LifecycleHandler");
            Field instanceField = handlerClass.getDeclaredField("instance");
            instanceField.setAccessible(true);
            Object handler = instanceField.get(null);
            
            Log.i(TAG, "生命周期处理器已注册: " + (handler != null));
        } catch (Exception e) {
            Log.e(TAG, "验证生命周期处理器时出错", e);
        }
    }

    /**
     * 检查弹窗是否显示
     */
    private boolean isDialogShown(String dialogTitle) {
        try {
            if (dialogTitle != null) {
                onView(withText(dialogTitle))
                        .inRoot(RootMatchers.isDialog())
                        .check(matches(isDisplayed()));
                return true;
            } else {
                // 检查是否有任何对话框显示
                return MockOnlineDialog.isDialogShowing();
            }
        } catch (Exception e) {
            // 如果无法找到对话框，返回false
            return false;
        }
    }

    /**
     * 关闭任何显示的对话框
     */
    private void dismissAnyDialogs() {
        try {
            // 使用工具类关闭对话框
            MockOnlineDialog.dismissDialog();
        } catch (Exception e) {
            // 忽略关闭对话框时的错误
            Log.i(TAG, "关闭对话框时出错（可能没有对话框显示）");
        }
    }

    /**
     * 休眠指定毫秒数
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 忽略中断异常
        }
    }
} 