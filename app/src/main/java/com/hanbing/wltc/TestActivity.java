package com.hanbing.wltc;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import android.os.Build;
import java.io.File;
import java.io.FileWriter;
import android.app.Activity;

import java.lang.reflect.Method;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TEST_MODULE";
    private TextView resultText;
    private Button testButton;
    private Button testLocalConfigButton;
    private Button testMultiLineButton;
    private Button testHanButton;
    private Button testC0001Button;
    private Button testDialogButton;
    private Button testGitcodeUrlButton;
    private Button showDialogDirectButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "TestActivity onCreate 开始");
            setContentView(R.layout.activity_test);
            
            // 初始化日志
            initLogToFile();
            Log.d(TAG, "日志初始化完成");
            
            // 初始化配置
            try {
                LocalConfig.init(this);
                Log.d(TAG, "LocalConfig初始化完成");
            } catch (Exception e) {
                Log.e(TAG, "LocalConfig初始化失败", e);
            }
            
            try {
                MultiLineManager.init(this);
                Log.d(TAG, "MultiLineManager初始化完成");
            } catch (Exception e) {
                Log.e(TAG, "MultiLineManager初始化失败", e);
            }
            
            // 查找各个控件
            try {
                resultText = findViewById(R.id.resultText);
                testButton = findViewById(R.id.testButton);
                testLocalConfigButton = findViewById(R.id.testLocalConfigButton);
                testMultiLineButton = findViewById(R.id.testMultiLineButton);
                testHanButton = findViewById(R.id.testHanButton);
                testC0001Button = findViewById(R.id.testC0001Button);
                testDialogButton = findViewById(R.id.testDialogButton);
                testGitcodeUrlButton = findViewById(R.id.testGitcodeUrlButton);
                showDialogDirectButton = findViewById(R.id.showDialogDirectButton);
                
                // 检查是否找到所有控件
                if (resultText == null) {
                    Log.e(TAG, "resultText为空");
                    Toast.makeText(this, "控件未找到: resultText", Toast.LENGTH_SHORT).show();
                }
                
                if (testButton == null) {
                    Log.e(TAG, "testButton为空");
                    Toast.makeText(this, "控件未找到: testButton", Toast.LENGTH_SHORT).show();
                }
                
                Log.d(TAG, "所有控件查找完成");
            } catch (Exception e) {
                Log.e(TAG, "查找控件时出错", e);
                Toast.makeText(this, "初始化界面失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
            // 设置按钮点击事件
            try {
                testButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(TestActivity.this, "开始所有测试...", Toast.LENGTH_SHORT).show();
                        runAllTests();
                    }
                });
                
                // 各个单独测试按钮
                testLocalConfigButton.setOnClickListener(v -> {
                    runSingleTest("LocalConfig", this::testLocalConfig);
                });
                
                testMultiLineButton.setOnClickListener(v -> {
                    runSingleTest("MultiLineManager", this::testMultiLineManager);
                });
                
                testHanButton.setOnClickListener(v -> {
                    runSingleTest("han测试", this::testHan);
                });
                
                testC0001Button.setOnClickListener(v -> {
                    runSingleTest("C0001对话框", this::testC0001);
                });
                
                testDialogButton.setOnClickListener(v -> {
                    runSingleTest("对话框", this::testOnlineDialog);
                });
                
                // 测试GitCode URL专用按钮
                if (testGitcodeUrlButton != null) {
                    testGitcodeUrlButton.setOnClickListener(v -> {
                        runSingleTest("GitCode URL测试", this::testGitcodeUrl);
                    });
                }
                
                // 直接显示对话框按钮
                if (showDialogDirectButton != null) {
                    showDialogDirectButton.setOnClickListener(v -> {
                        showDialogDirect();
                    });
                }
                
                Log.d(TAG, "所有按钮事件设置完成");
            } catch (Exception e) {
                Log.e(TAG, "设置按钮事件出错", e);
                Toast.makeText(this, "初始化事件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
            Log.d(TAG, "TestActivity onCreate 完成");
            
            // 显示初始化完成的提示信息
            Toast.makeText(this, "测试界面已准备好", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "TestActivity onCreate 异常", e);
            Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
} // 添加缺失的右大括号
