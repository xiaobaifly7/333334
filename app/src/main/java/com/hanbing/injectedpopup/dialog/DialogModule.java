package com.hanbing.injectedpopup.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.hanbing.injectedpopup.config.ConfigModule; // Import ConfigModule for prefs access

import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * 对话框模块
 * 负责根据解析后的配置数据显示 AlertDialog
 */
public class DialogModule {

    private static final String TAG = "DialogModule";
    private static AlertDialog currentDialog = null; // Track the current dialog

    /**
     * 显示对话框
     * @param activity 当前 Activity
     * @param config   解析后的 JSON 配置对象
     */
    public static void showDialog(final Activity activity, final JSONObject config) {
        if (activity == null || activity.isFinishing() || config == null) {
            Log.w(TAG, "Activity is null/finishing or config is null, cannot show dialog.");
            return;
        }

        // Ensure dialog operations run on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Dismiss any existing dialog first
                dismissCurrentDialog();

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                // 1. Set Title and Message (handle potential HTML)
                String title = config.optString("title", "提示"); // Default title
                String message = config.optString("message", ""); // Default empty message
                try {
                    builder.setTitle(Html.fromHtml(title));
                    builder.setMessage(Html.fromHtml(message));
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing HTML in title/message, using plain text.", e);
                    builder.setTitle(title);
                    builder.setMessage(message);
                }

                // 2. Set Buttons and Actions
                JSONObject positiveButtonConfig = config.optJSONObject("positiveButton");
                if (positiveButtonConfig != null && positiveButtonConfig.optString("text", "").length() > 0) {
                    builder.setPositiveButton(positiveButtonConfig.optString("text", "确定"),
                            (dialog, which) -> handleAction(activity, positiveButtonConfig, dialog));
                }

                JSONObject negativeButtonConfig = config.optJSONObject("negativeButton");
                if (negativeButtonConfig != null && negativeButtonConfig.optString("text", "").length() > 0) {
                    builder.setNegativeButton(negativeButtonConfig.optString("text", "取消"),
                            (dialog, which) -> handleAction(activity, negativeButtonConfig, dialog));
                }

                JSONObject neutralButtonConfig = config.optJSONObject("neutralButton");
                if (neutralButtonConfig != null && neutralButtonConfig.optString("text", "").length() > 0) {
                    builder.setNeutralButton(neutralButtonConfig.optString("text", ""),
                            (dialog, which) -> handleAction(activity, neutralButtonConfig, dialog));
                }

                // 3. Set Behavior
                JSONObject behaviorConfig = config.optJSONObject("behavior");
                boolean cancelable = true; // Default to cancelable
                boolean dimBackground = true; // Default to dim background
                if (behaviorConfig != null) {
                    cancelable = behaviorConfig.optBoolean("cancelable", true);
                    dimBackground = behaviorConfig.optBoolean("dimBackground", true);
                }
                builder.setCancelable(cancelable);


                // 4. Create and Show Dialog
                currentDialog = builder.create();
                currentDialog.setCanceledOnTouchOutside(cancelable); // Match cancelable setting

                // Set Dim Amount
                if (!dimBackground && currentDialog.getWindow() != null) {
                    try {
                        currentDialog.getWindow().setDimAmount(0.0f); // 0.0f means no dimming
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to set dim amount", e);
                    }
                }

                currentDialog.show();
                Log.d(TAG, "Dialog shown successfully.");

                // 5. Apply Styles (Colors) after showing
                applyStyles(currentDialog, config.optJSONObject("style"));

            } catch (Exception e) {
                Log.e(TAG, "Error showing dialog", e);
            }
        });
    }

    /**
     * Dismiss the currently showing dialog, if any.
     */
    private static void dismissCurrentDialog() {
         if (currentDialog != null && currentDialog.isShowing()) {
            try {
                currentDialog.dismiss();
                Log.d(TAG, "Dismissed previous dialog.");
            } catch (Exception e) {
                Log.w(TAG, "Error dismissing previous dialog", e);
            } finally {
                currentDialog = null;
            }
        }
    }


    /**
     * 处理按钮点击动作
     */
    private static void handleAction(Activity activity, JSONObject buttonConfig, DialogInterface dialog) {
        if (buttonConfig == null) {
            dialog.dismiss(); // Default action: dismiss
            return;
        }

        String action = buttonConfig.optString("action", "dismiss"); // Default to dismiss
        String data = buttonConfig.optString("data", "");

        Log.d(TAG, "Handling action: " + action + ", data: " + data);

        try {
            switch (action) {
                case "open_url":
                    if (!data.isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "No activity found to handle URL: " + data, e);
                            Toast.makeText(activity, "无法打开链接", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening URL: " + data, e);
                            Toast.makeText(activity, "打开链接失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "open_url action missing data.");
                    }
                    dialog.dismiss(); // Dismiss after attempting action
                    break;

                case "copy_text":
                    if (!data.isEmpty()) {
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            ClipData clip = ClipData.newPlainText("copied_text", data);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "Text copied to clipboard.");
                        } else {
                             Log.e(TAG, "Clipboard service not available.");
                        }
                    } else {
                        Log.w(TAG, "copy_text action missing data.");
                    }
                    // Keep dialog open after copying? Or dismiss? Current: dismiss.
                    dialog.dismiss();
                    break;

                case "ignore_version":
                    if (!data.isEmpty()) {
                        ConfigModule.getInstance(activity.getApplicationContext()).ignoreVersion(data);
                    } else {
                        Log.w(TAG, "ignore_version action missing version data.");
                    }
                    dialog.dismiss();
                    break;

                case "dismiss":
                default:
                    dialog.dismiss();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + action, e);
            dialog.dismiss(); // Ensure dialog is dismissed on error
        } finally {
             if (action.equals("dismiss") || action.equals("ignore_version") || action.startsWith("open_url") || action.startsWith("copy_text")) {
                 // Ensure dialog is dismissed for these actions even if errors occurred before dismiss()
                 try {
                     if (dialog.isShowing()) {
                         dialog.dismiss();
                     }
                 } catch (Exception ignored) {}
             }
             currentDialog = null; // Clear reference after dismissal
        }
    }

    /**
     * 应用对话框样式 (主要是颜色)
     */
    private static void applyStyles(AlertDialog dialog, JSONObject styleConfig) {
        if (styleConfig == null || dialog == null || !dialog.isShowing()) {
            return;
        }

        // Apply title color
        try {
            String titleColorStr = styleConfig.optString("titleColor");
            if (!titleColorStr.isEmpty()) {
                 Field alertField = AlertDialog.class.getDeclaredField("mAlert");
                 alertField.setAccessible(true);
                 Object alertController = alertField.get(dialog);
                 Field titleViewField = alertController.getClass().getDeclaredField("mTitleView");
                 titleViewField.setAccessible(true);
                 TextView titleView = (TextView) titleViewField.get(alertController);
                 if (titleView != null) {
                     titleView.setTextColor(Color.parseColor(titleColorStr));
                 }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply title color", e);
        }

        // Apply message color
        try {
            String messageColorStr = styleConfig.optString("messageColor");
             if (!messageColorStr.isEmpty()) {
                 Field alertField = AlertDialog.class.getDeclaredField("mAlert");
                 alertField.setAccessible(true);
                 Object alertController = alertField.get(dialog);
                 Field messageViewField = alertController.getClass().getDeclaredField("mMessageView");
                 messageViewField.setAccessible(true);
                 TextView messageView = (TextView) messageViewField.get(alertController);
                 if (messageView != null) {
                     messageView.setTextColor(Color.parseColor(messageColorStr));
                 }
             }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply message color", e);
        }

        // Apply button colors
        try {
            String positiveColorStr = styleConfig.optString("positiveButtonColor");
            if (!positiveColorStr.isEmpty() && dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor(positiveColorStr));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply positive button color", e);
        }
        try {
            String negativeColorStr = styleConfig.optString("negativeButtonColor");
            if (!negativeColorStr.isEmpty() && dialog.getButton(DialogInterface.BUTTON_NEGATIVE) != null) {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor(negativeColorStr));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply negative button color", e);
        }
        try {
            String neutralColorStr = styleConfig.optString("neutralButtonColor");
            if (!neutralColorStr.isEmpty() && dialog.getButton(DialogInterface.BUTTON_NEUTRAL) != null) {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.parseColor(neutralColorStr));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply neutral button color", e);
        }
    }
}
