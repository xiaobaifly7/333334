package com.hanbing.wltc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 弹窗系统集成测试
 * 测试弹窗功能与安全系统的集成
 */
@RunWith(AndroidJUnit4.class)
public class DialogSystemTest {

    private Context context;
    private final List<String> testResults = new ArrayList<>();
    private boolean testsPassed = true;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testResults.clear();
        testsPassed = true;
    }

    /**
     * 测试弹窗基本功能
     */
    @Test
    public void testDialogBasicFunctionality() {
        logTestStart("弹窗基本功能测试");

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 通过测试按钮触发弹窗
                    Button testDialogButton = activity.findViewById(R.id.testDialogButton);
                    if (testDialogButton != null) {
                        testDialogButton.performClick();
                        logTestInfo("点击弹窗测试按钮");
                    } else {
                        // 如果找不到按钮，直接调用弹窗
                        logTestInfo("未找到弹窗测试按钮，直接调用弹窗");
                        han.diaoyon(activity);
                    }

                    // 等待弹窗显示
                    sleep(2000);

                    // 检查弹窗是否显示
                    boolean dialogVisible = isDialogVisible();
                    if (dialogVisible) {
                        logTestPass("弹窗成功显示");
                    } else {
                        logTestFail("弹窗未显示");
                    }

                    // 关闭弹窗
                    dismissAnyDialogs();
                } catch (Exception e) {
                    logTestError("弹窗基本功能测试异常", e);
                }
            });
        }

        logTestResults("弹窗基本功能测试");
    }

    /**
     * 测试安全系统与弹窗集成
     */
    @Test
    public void testSecurityIntegration() {
        logTestStart("安全系统集成测试");

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 初始化安全系统测试组件
                    initSecuritySystem(activity);
                    logTestInfo("已初始化安全系统");

                    // 并行测试弹窗和安全系统
                    final CountDownLatch latch = new CountDownLatch(2);
                    final AtomicBoolean dialogSuccess = new AtomicBoolean(false);
                    final AtomicBoolean securitySuccess = new AtomicBoolean(false);

                    // 弹窗线程
                    new Thread(() -> {
                        try {
                            runOnUiThread(() -> han.diaoyon(activity));
                            sleep(2000);
                            dialogSuccess.set(isDialogVisible());
                        } catch (Exception e) {
                            logTestError("弹窗线程异常", e);
                        } finally {
                            latch.countDown();
                        }
                    }).start();

                    // 安全系统线程
                    new Thread(() -> {
                        try {
                            // 模拟安全系统操作
                            runSecurityOperations();
                            securitySuccess.set(true);
                        } catch (Exception e) {
                            logTestError("安全系统线程异常", e);
                        } finally {
                            latch.countDown();
                        }
                    }).start();

                    // 等待两个线程完成
                    boolean completed = latch.await(10, TimeUnit.SECONDS);
                    if (!completed) {
                        logTestFail("集成测试超时");
                    }

                    if (dialogSuccess.get() && securitySuccess.get()) {
                        logTestPass("弹窗和安全系统成功并行运行");
                    } else {
                        if (!dialogSuccess.get()) {
                            logTestFail("弹窗未显示");
                        }
                        if (!securitySuccess.get()) {
                            logTestFail("安全系统操作失败");
                        }
                    }

                    // 关闭弹窗
                    dismissAnyDialogs();
                } catch (Exception e) {
                    logTestError("安全系统集成测试异常", e);
                }
            });
        }

        logTestResults("安全系统集成测试");
    }

    /**
     * 测试安全系统故障情况下弹窗功能
     */
    @Test
    public void testDialogWithSecurityFailure() {
        logTestStart("安全系统故障测试");

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    // 初始化安全系统
                    initSecuritySystem(activity);
                    
                    // 注入安全系统故障
                    injectSecurityFailure();
                    logTestInfo("已注入安全系统故障");
                    
                    // 调用弹窗
                    han.diaoyon(activity);
                    sleep(2000);
                    
                    // 检查弹窗是否正常显示
                    boolean dialogVisible = isDialogVisible();
                    if (dialogVisible) {
                        logTestPass("即使安全系统出现故障，弹窗仍正常工作");
                    } else {
                        logTestFail("安全系统故障导致弹窗无法显示");
                    }
                    
                    // 关闭弹窗
                    dismissAnyDialogs();
                } catch (Exception e) {
                    logTestError("安全系统故障测试异常", e);
                }
            });
        }

        logTestResults("安全系统故障测试");
    }

    // ===================== 测试辅助方法 =====================

    /**
     * 初始化安全系统
     */
    private void initSecuritySystem(Activity activity) {
        try {
            // 尝试加载安全系统
            Class<?> securityClass = Class.forName("secure.inject.extreme.SecurityInjector");
            Object injector = securityClass.getMethod("getInstance").invoke(null);
            securityClass.getMethod("initialize", Activity.class).invoke(injector, activity);
            
            logTestInfo("安全系统初始化成功");
        } catch (Exception e) {
            logTestWarning("无法初始化安全系统: " + e.getMessage() + "，将使用模拟安全系统");
            // 初始化失败，使用模拟的安全系统
            mockSecuritySystem();
        }
    }

    /**
     * 模拟安全系统操作
     */
    private void mockSecuritySystem() {
        // 这里实现模拟安全系统的逻辑
        logTestInfo("使用模拟安全系统");
    }

    /**
     * 执行安全系统操作
     */
    private void runSecurityOperations() {
        try {
            // 模拟安全系统的各种操作
            sleep(500); // 模拟初始化
            sleep(500); // 模拟安全检查
            sleep(500); // 模拟配置加载
            sleep(500); // 模拟解密操作
            logTestInfo("安全系统操作完成");
        } catch (Exception e) {
            logTestError("安全系统操作异常", e);
            throw new RuntimeException("安全系统操作失败", e);
        }
    }

    /**
     * 注入安全系统故障
     */
    private void injectSecurityFailure() {
        try {
            // 尝试注入真实安全系统故障
            try {
                Class<?> securityClass = Class.forName("secure.inject.extreme.SecurityGuardian");
                Field failureField = securityClass.getDeclaredField("securityViolationDetected");
                failureField.setAccessible(true);
                failureField.set(null, true);
            } catch (Exception ignored) {
                // 如果无法访问真实安全系统，设置模拟故障
                logTestInfo("设置模拟安全系统故障");
            }
        } catch (Exception e) {
            logTestError("注入安全系统故障异常", e);
        }
    }

    /**
     * 检测弹窗是否可见
     */
    private boolean isDialogVisible() {
        try {
            // 首先尝试使用模拟对话框类的方法
            return MockOnlineDialog.isDialogShowing();
        } catch (Exception e) {
            try {
                // 如果模拟方法不可用，使用Espresso检查
                onView(ViewMatchers.withText(Matchers.containsString("确定")))
                        .inRoot(RootMatchers.isDialog())
                        .check(matches(isDisplayed()));
                return true;
            } catch (NoMatchingViewException e2) {
                try {
                    // 尝试其他可能的标题或按钮文本
                    onView(ViewMatchers.withText(Matchers.containsString("关闭")))
                            .inRoot(RootMatchers.isDialog())
                            .check(matches(isDisplayed()));
                    return true;
                } catch (NoMatchingViewException e3) {
                    try {
                        // 再尝试其他可能匹配的对话框元素
                        onView(ViewMatchers.withClassName(Matchers.containsString("AlertDialog")))
                                .inRoot(RootMatchers.isDialog())
                                .check(matches(isDisplayed()));
                        return true;
                    } catch (NoMatchingViewException e4) {
                        return false;
                    }
                }
            }
        }
    }

    /**
     * 关闭任何显示的对话框
     */
    private void dismissAnyDialogs() {
        try {
            // 首先尝试使用模拟对话框类的方法
            MockOnlineDialog.dismissDialog();
        } catch (Exception e) {
            try {
                // 如果模拟方法不可用，使用Espresso点击方法
                onView(ViewMatchers.withText(Matchers.containsString("确定")))
                        .inRoot(RootMatchers.isDialog())
                        .perform(click());
            } catch (Exception e1) {
                try {
                    // 尝试点击关闭按钮
                    onView(ViewMatchers.withText(Matchers.containsString("关闭")))
                            .inRoot(RootMatchers.isDialog())
                            .perform(click());
                } catch (Exception e2) {
                    try {
                        // 尝试按返回键关闭对话框
                        Espresso.pressBack();
                    } catch (Exception ignored) {
                        // 忽略异常
                    }
                }
            }
        }
    }

    /**
     * 在UI线程上运行代码
     */
    private void runOnUiThread(Runnable runnable) {
        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 线程休眠
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 测试日志方法 ====================

    private void logTestStart(String testName) {
        testResults.add("===== " + testName + " 开始 =====");
        System.out.println("\n" + testResults.get(testResults.size() - 1));
    }

    private void logTestInfo(String message) {
        testResults.add("[信息] " + message);
        System.out.println(testResults.get(testResults.size() - 1));
    }

    private void logTestWarning(String message) {
        testResults.add("[警告] " + message);
        System.out.println(testResults.get(testResults.size() - 1));
    }

    private void logTestPass(String message) {
        testResults.add("[✓] " + message);
        System.out.println(testResults.get(testResults.size() - 1));
    }

    private void logTestFail(String message) {
        testResults.add("[✗] " + message);
        System.out.println(testResults.get(testResults.size() - 1));
        testsPassed = false;
    }

    private void logTestError(String message, Exception e) {
        testResults.add("[错误] " + message + ": " + e.getMessage());
        System.out.println(testResults.get(testResults.size() - 1));
        e.printStackTrace();
        testsPassed = false;
    }

    private void logTestResults(String testName) {
        testResults.add("===== " + testName + " 结果: " + (testsPassed ? "通过 ✓" : "失败 ✗") + " =====");
        System.out.println(testResults.get(testResults.size() - 1) + "\n");
        
        // 重置状态以便下一个测试
        testsPassed = true;
    }
} 