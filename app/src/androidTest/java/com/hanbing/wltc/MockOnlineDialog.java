package com.hanbing.wltc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 模拟OnlineDialog类，用于测试目的
 */
public class MockOnlineDialog extends AsyncTask<String, Exception, String> implements DialogInterface.OnClickListener {
    private static final String TAG = "MockOnlineDialog";
    private Activity activity;
    private String title;
    private String message;
    private String posBtn;
    private String negBtn;
    private String posColor;
    private String negColor;
    private boolean parseSuccess = false;
    
    // 添加静态变量
    private static AlertDialog staticDialog;
    private static boolean isShowing = false;

    /**
     * 创建一个模拟的OnlineDialog实例
     * @param activity 活动上下文
     */
    public MockOnlineDialog(Activity activity) {
        this.activity = activity;
        Log.d(TAG, "创建MockOnlineDialog实例");
    }
    
    /**
     * 静态方法显示弹窗
     * @param activity 活动上下文
     * @param url 配置URL
     */
    public static void show(Activity activity, String url) {
        Log.d(TAG, "静态方法show被调用: " + url);
        MockOnlineDialog dialog = new MockOnlineDialog(activity);
        dialog.execute(url);
    }
    
    /**
     * 检查弹窗是否显示
     * @return 弹窗是否显示
     */
    public static boolean isDialogShowing() {
        return isShowing && staticDialog != null && staticDialog.isShowing();
    }
    
    /**
     * 关闭弹窗
     */
    public static void dismissDialog() {
        if (staticDialog != null && staticDialog.isShowing()) {
            staticDialog.dismiss();
            isShowing = false;
            Log.d(TAG, "弹窗已关闭");
        }
    }

    /**
     * 后台处理方法，获取配置
     */
    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, "开始后台处理");
        try {
            // 模拟网络延迟
            Thread.sleep(1000);
            
            // 如果有URL参数，则模拟从URL获取数据
            if (params != null && params.length > 0 && params[0] != null) {
                Log.d(TAG, "正在从URL获取数据: " + params[0]);
                return DialogMockUtils.createTestConfig(
                        "测试弹窗", 
                        "这是从URL加载的测试内容", 
                        "确定", 
                        "取消", 
                        "#FF0000", 
                        "#00FF00");
            }
            
            // 没有参数时返回默认测试配置
            return DialogMockUtils.createTestConfig(
                    "默认测试弹窗", 
                    "这是默认测试内容", 
                    "确定", 
                    "取消", 
                    "#FF0000", 
                    "#00FF00");
        } catch (Exception e) {
            Log.e(TAG, "后台处理出错", e);
            return null;
        }
    }

    /**
     * 解析配置内容
     */
    protected boolean parseResult(String result) {
        Log.d(TAG, "解析结果: " + result);
        if (result == null || result.isEmpty()) {
            return false;
        }

        try {
            // 解析标签内容
            title = parseTag(result, "title");
            message = parseTag(result, "message");
            posBtn = parseTag(result, "posbtn");
            negBtn = parseTag(result, "negbtn");
            posColor = parseTag(result, "poscolor");
            negColor = parseTag(result, "negcolor");
            
            // 只要有标题和内容就算解析成功
            parseSuccess = !title.isEmpty() && !message.isEmpty();
            return parseSuccess;
        } catch (Exception e) {
            Log.e(TAG, "解析结果出错", e);
            return false;
        }
    }

    /**
     * 从标签中提取内容
     */
    private String parseTag(String content, String tag) {
        try {
            String startTag = "〈" + tag + "〉";
            String endTag = "〈/" + tag + "〉";
            
            int startPos = content.indexOf(startTag);
            int endPos = content.indexOf(endTag);
            
            if (startPos != -1 && endPos != -1) {
                return content.substring(startPos + startTag.length(), endPos);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析标签出错: " + tag, e);
        }
        return "";
    }

    /**
     * 后处理方法，显示弹窗
     */
    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "后处理开始");
        if (result != null && !result.isEmpty()) {
            // 解析结果
            if (parseResult(result)) {
                showDialog();
            } else {
                Log.e(TAG, "解析失败，无法显示弹窗");
            }
        } else {
            Log.e(TAG, "结果为空，无法显示弹窗");
        }
    }

    /**
     * 显示弹窗
     */
    private void showDialog() {
        try {
            // 如果活动已销毁，则不显示弹窗
            if (activity == null || activity.isFinishing()) {
                Log.e(TAG, "活动已销毁，无法显示弹窗");
                return;
            }
            
            // 创建并显示弹窗
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(title);
            builder.setMessage(message);
            
            if (posBtn != null && !posBtn.isEmpty()) {
                builder.setPositiveButton(posBtn, this);
            }
            
            if (negBtn != null && !negBtn.isEmpty()) {
                builder.setNegativeButton(negBtn, this);
            }
            
            AlertDialog dialog = builder.create();
            dialog.show();
            
            // 设置静态引用和状态
            staticDialog = dialog;
            isShowing = true;
            
            // 设置按钮颜色
            if (posColor != null && !posColor.isEmpty() && dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor(posColor));
            }
            
            if (negColor != null && !negColor.isEmpty() && dialog.getButton(DialogInterface.BUTTON_NEGATIVE) != null) {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor(negColor));
            }
            
            Log.d(TAG, "弹窗显示成功");
        } catch (Exception e) {
            Log.e(TAG, "显示弹窗出错", e);
        }
    }

    /**
     * 点击事件处理
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Log.d(TAG, "点击了确定按钮");
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Log.d(TAG, "点击了取消按钮");
        }
        dialog.dismiss();
        isShowing = false;
    }
} 