package com.example.onlinedialogdemo2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义对话框演示 - 测试用
 */
public class CustomDialogDemo {
    private static final String TAG = "CustomDialogDemo";
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 显示对话框
     * @param context 上下文
     */
    public static void showDialog(Context context) {
        Log.i(TAG, "开始显示自定义对话框");

        // 测试配置字符串
        String testConfig = "标题=测试标题 标题颜色=#FF0000/内容 内容=这是测试内容/内容字号 内容字号=8.0sp/颜色 颜色=默认颜色设置/背景 背景颜色=#DAA520/按钮文本 按钮文本=确定按钮<br>取消按钮/颜色 按钮颜色=#000000/按钮类型 按钮类型=双按钮/按钮位置 按钮位置=底部居中对齐/标题栏设置 标题栏背景色=#FF5E5555/标题栏高度 标题栏高度=标准高度/标题栏边距 标题栏边距=默认边距/内容区设置 内容区背景色=#DAA520/内容区链接 内容区链接地址=https://example.com/内容区边距 内容区边距=默认/主题设置";

        Map<String, String> configMap = parseConfig(testConfig);
        Log.i(TAG, "解析配置结果: " + configMap.toString());

        // 在主线程中显示对话框
        new Handler(Looper.getMainLooper()).post(() -> {
            Log.i(TAG, "在主线程中创建对话框");
            Dialog dialog = createDialog(context, configMap);
            dialog.show();
            Log.i(TAG, "对话框已显示");
        });
    }

    /**
     * 解析配置字符串
     * @param config 配置字符串
     * @return 配置映射
     */
    private static Map<String, String> parseConfig(String config) {
        Map<String, String> configMap = new HashMap<>();

        try {
            // 简单的配置解析
            String[] items = config.split("/");
            for (String item : items) {
                String[] parts = item.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    configMap.put(key, value);
                    Log.d(TAG, "解析配置项: " + key + " = " + value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析配置出错", e);
        }

        return configMap;
    }

    /**
     * 创建对话框
     * @param context 上下文
     * @param configMap 配置映射
     * @return 对话框实例
     */
    private static Dialog createDialog(Context context, Map<String, String> configMap) {
        Log.i(TAG, "创建对话框");
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        // 加载视图
        View view = LayoutInflater.from(context).inflate(R.layout.activity_custom_dialog_demo, null);

        // 设置标题
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        titleTextView.setText(configMap.getOrDefault("标题", "提示"));
        String titleColor = configMap.getOrDefault("标题颜色", "#000000");
        titleTextView.setTextColor(Color.parseColor(titleColor));

        // 设置内容
        TextView contentTextView = view.findViewById(R.id.contentTextView);
        contentTextView.setText(Html.fromHtml(configMap.getOrDefault("内容", "")));
        String contentColor = configMap.getOrDefault("内容颜色", "#000000");
        contentTextView.setTextColor(Color.parseColor(contentColor));

        // 设置按钮
        LinearLayout buttonLayout = view.findViewById(R.id.buttonLayout);
        buttonLayout.removeAllViews();

        // 添加确认按钮
        if ("双按钮".equals(configMap.getOrDefault("按钮类型", "单按钮"))) {
            Button agreeButton = new Button(context);
            agreeButton.setText(configMap.getOrDefault("按钮文本", "确定"));
            String btnColor = configMap.getOrDefault("按钮颜色", "#FF5E5555");
            agreeButton.setTextColor(Color.parseColor(btnColor));
            agreeButton.setOnClickListener(v -> {
                Log.i(TAG, "点击确认按钮");
                dialog.dismiss();
                Toast.makeText(context, "确认操作完成", Toast.LENGTH_SHORT).show();
            });
            buttonLayout.addView(agreeButton);
        }

        // 添加取消按钮
        if ("双按钮".equals(configMap.getOrDefault("按钮类型", "单按钮"))) {
            Button rejectButton = new Button(context);
            rejectButton.setText(configMap.getOrDefault("取消按钮文本", "取消"));
            String btnColor = configMap.getOrDefault("取消按钮颜色", "#000000");
            rejectButton.setTextColor(Color.parseColor(btnColor));
            rejectButton.setOnClickListener(v -> {
                Log.i(TAG, "点击取消按钮");
                dialog.dismiss();
                Toast.makeText(context, "取消操作完成", Toast.LENGTH_SHORT).show();
            });
            buttonLayout.addView(rejectButton);
        }

        dialog.setContentView(view);
        return dialog;
    }
}