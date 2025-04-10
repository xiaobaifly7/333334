package mutil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 在线对话框处理类 (可能来自反编译)
 * 继承自 AsyncTask，用于异步获取配置并显示 AlertDialog。
 */
public class OnlineDialog extends AsyncTask<String, Exception, C0001> implements DialogInterface.OnClickListener {

    private static final String TAG = "OnlineDialog";

    // SharedPreferences 实例，用于存储配置相关信息
    private static SharedPreferences prefs;

    // 当前 Activity 的引用 (设为 public static 以便 DialogInterfaceOnClickListenerC0000 访问)
    public static Activity currentActivity;

    // 是否启用加密模式 (似乎用于判断输入是否以 "Eh" 开头)
    private static boolean isEncryptedMode = false;

    // OnlineDialog 的静态实例 (用于 show 方法)
    private static OnlineDialog staticInstance = new OnlineDialog();

    static {
        Log.d(TAG, "6.5"); // 版本号?
    }

    /**
     * 显示对话框的静态入口方法
     * @param activity 当前 Activity
     * @param str 配置字符串或 URL (可能经过 Base64 编码或添加 "Eh" 前缀表示加密)
     */
    public static final void show(Activity activity, String str) {
        String configInput = str; // Use a more descriptive name
        currentActivity = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        try {
            // 尝试 Base64 解码，如果包含 http 则认为是解码后的 URL
            String decodedStr = new String(Base64.decode(configInput.getBytes("UTF-8"), 0), "UTF-8");
            if (decodedStr.contains("ttp")) { // 检查是否包含 http 或 https
                configInput = decodedStr;
            }
        } catch (Exception e) {
            // 忽略解码异常
        }
        try {
            // 检查是否以 "Eh" 开头来判断是否加密 (简单方式)
            isEncryptedMode = configInput.startsWith("Eh");
            Class.forName("android.os.AsyncTask"); // 确保 AsyncTask 类存在
            // 移除可能的 "E" 前缀并执行 AsyncTask
            staticInstance = new OnlineDialog(); // Re-create instance for each call? Original code used f0.execute
            staticInstance.execute(configInput.replace("Ehttp", "http"));
        } catch (Exception e2) {
            Log.e(TAG, "执行 AsyncTask 出错", e2);
        }
    }

    /**
     * 总是显示对话框的静态入口方法 (每次都创建新实例)
     * @param activity 当前 Activity
     * @param str 配置字符串或 URL
     */
    public static final void showAlways(Activity activity, String str) {
        String configInput = str;
        currentActivity = activity;
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        try {
            // 尝试 Base64 解码
            String decodedStr = new String(Base64.decode(configInput.getBytes("UTF-8"), 0), "UTF-8");
            if (decodedStr.contains("ttp")) {
                configInput = decodedStr;
            }
        } catch (Exception e) {
            // 忽略解码异常
        }
        try {
            isEncryptedMode = configInput.startsWith("Eh");
            Class.forName("android.os.AsyncTask");
            new OnlineDialog().execute(configInput.replace("Ehttp", "http")); // 创建新实例执行
        } catch (Exception e2) {
             Log.e(TAG, "执行 AsyncTask (showAlways) 出错", e2);
        }
    }

    /**
     * AsyncTask 后台任务：获取并解析配置
     * @param strArr 配置字符串或 URL
     * @return 解析后的配置对象 C0001，如果失败则返回 null
     */
    @Override
    public C0001 doInBackground(String... strArr) {
        String configContent;
        String input = strArr[0];

        try {
            // 如果输入包含 URL 标记，则从网络获取
            if (input.contains("http://") || input.contains("https://")) {
                configContent = fetchContentFromUrl(input); // Renamed m6
            } else {
                configContent = input; // 直接使用输入作为配置内容
            }

            // 如果启用了加密模式 (isEncryptedMode)，则解密内容
            if (isEncryptedMode) {
                configContent = decryptString(configContent); // Renamed m4
            }

            // 如果内容为空，返回 null
            if (configContent == null || configContent.isEmpty()) {
                Log.w(TAG, "获取或解密后的配置内容为空");
                return null;
            }

            // 创建 C0001 对象来解析配置
            return new C0001(configContent);

        } catch (Exception e) {
            Log.e(TAG, "doInBackground 失败", e);
            publishProgress(e); // 将异常传递给 onProgressUpdate
            return null;
        }
    }


    /**
     * 处理特定前缀的字符串（可能是跳转 QQ 或其他应用）
     * @param actionString 待处理的字符串
     */
    private final void handleQQAction(String actionString) { // Renamed m3
        // 注意：decryptString 解密硬编码的字符串来获取前缀
        if (actionString.contains(decryptString("rOHJyHciQlHptt+6NeNqoQ=="))) { // 可能解密为 "QQ卡片" 或类似标识
            String qqNumber = actionString;
            try {
                // 如果包含消息标记 ("//")，显示 Toast
                if (actionString.contains(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) {
                    Toast.makeText(currentActivity, actionString.substring(actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")) + 2), Toast.LENGTH_LONG).show();
                    qqNumber = actionString.substring(0, actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")));
                }
                // 尝试打开 QQ 名片 (假设前4位是前缀)
                currentActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("mqqapi://card/show_pslcard?src_type=internal&source=sharecard&version=1&uin=" + qqNumber.substring(4))));
                return;
            } catch (Exception e) {
                if (e instanceof ActivityNotFoundException) {
                    Toast.makeText(currentActivity, decryptString("oEJ+jLBJm923j7SrewvVASgQo64qdqnwXdW+1lH+mnI="), Toast.LENGTH_SHORT).show(); // 可能解密为 "未安装手机QQ"
                } else {
                    Toast.makeText(currentActivity, e.toString(), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
        if (actionString.contains(decryptString("QIbocZSbLUvlFWmWQ08Jmw=="))) { // 可能解密为 "QQ群" 或类似标识
            String groupNumber = actionString;
            try {
                if (actionString.contains(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) {
                    Toast.makeText(currentActivity, actionString.substring(actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")) + 2), Toast.LENGTH_LONG).show();
                    groupNumber = actionString.substring(0, actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")));
                }
                // 尝试打开 QQ 群名片 (假设前3位是前缀)
                currentActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + groupNumber.substring(3) + "&card_type=group&source=qrcode")));
                return;
            } catch (Exception e2) {
                if (e2 instanceof ActivityNotFoundException) {
                    Toast.makeText(currentActivity, decryptString("oEJ+jLBJm923j7SrewvVASgQo64qdqnwXdW+1lH+mnI="), Toast.LENGTH_SHORT).show(); // "未安装手机QQ"
                } else {
                    Toast.makeText(currentActivity, e2.toString(), Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
        if (actionString.contains(decryptString("99ei4uKKBoUJSsRHplIABg=="))) { // 可能解密为 "QQ临时会话" 或类似标识
            String tempChatNumber = actionString;
            try {
                if (actionString.contains(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) {
                    Toast.makeText(currentActivity, actionString.substring(actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")) + 2), Toast.LENGTH_LONG).show();
                    tempChatNumber = actionString.substring(0, actionString.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")));
                }
                // 尝试打开 QQ 临时会话 (假设前4位是前缀)
                currentActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("mqqwpa://im/chat?chat_type=wpa&uin=" + tempChatNumber.substring(4) + "&version=1&src_type=web")));
            } catch (Exception e3) {
                if (e3 instanceof ActivityNotFoundException) {
                    Toast.makeText(currentActivity, decryptString("oEJ+jLBJm923j7SrewvVASgQo64qdqnwXdW+1lH+mnI="), Toast.LENGTH_SHORT).show(); // "未安装手机QQ"
                } else {
                    Toast.makeText(currentActivity, e3.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * 解密字符串 (使用硬编码的密钥和盐值进行 PBE 和 AES 解密)
     * @param encryptedBase64 Base64 编码的密文
     * @return 解密后的明文，如果失败返回原始输入
     */
    private static String decryptString(String encryptedBase64) { // Renamed m4
        String originalString = encryptedBase64;
        byte[] decryptedBytes = null;
        try {
            // 使用 PBEKeySpec 从密码 "qyma" 和盐 "$9s1{;1H" 生成 AES 密钥
            SecretKeySpec secretKeySpec = new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec("qyma".toCharArray(), "$9s1{;1H".getBytes("UTF-8"), 5, 256)).getEncoded(), "AES");
            // 使用 AES/CBC/PKCS5Padding 进行解密，IV 是硬编码的 "F-=5!2]/9G(<(=uY"
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec("F-=5!2]/9G(<(=uY".getBytes("UTF-8")));
            decryptedBytes = cipher.doFinal(Base64.decode(originalString, 0)); // Base64 解码密文
        } catch (Exception e) {
            Log.e(TAG, "解密失败 (decryptString)", e);
        }
        if (decryptedBytes != null) {
            try {
                originalString = new String(decryptedBytes, "UTF-8"); // 将解密后的字节数组转为 UTF-8 字符串
            } catch (Exception e) {
                 Log.e(TAG, "解密后字节转字符串失败 (decryptString)", e);
                 // Fallback or return original string? Current logic returns original.
            }
        }
        return originalString;
    }

    /**
     * 从配置字符串中提取指定键的值 (使用特定标记 "〈key〉...〈/key〉")
     * @param key 键名
     * @return 提取到的值，如果找不到或出错则返回空字符串
     */
    private final String parseValueFromConfig(String key) { // Renamed m5
        String value = "";
        if (key != null && !key.trim().isEmpty() && C0001.rawConfigContent != null && !C0001.rawConfigContent.isEmpty()) {
            try {
                // 查找标记 "〈key〉" 和 "〈/key〉"
                String startTag = "〈" + key + "〉";
                String endTag = "〈/" + key + "〉";
                String configString = C0001.rawConfigContent;
                int startIndex = configString.indexOf(startTag);
                int endIndex = configString.indexOf(endTag);
                // 提取标记之间的内容
                if (startIndex >= 0 && endIndex > startIndex) {
                    value = configString.substring(startIndex + startTag.length(), endIndex);
                } else {
                     Log.w(TAG, "在配置中未找到标记: " + key);
                }
            } catch (Exception e) {
                Log.e(TAG, "提取配置项 '" + key + "' 时出错", e);
                publishProgress(e); // 报告异常
            }
        }
        return value;
    }

    /**
     * 从 URL 获取内容 (HTTP GET)
     * @param urlString URL 字符串
     * @return URL 返回的内容，如果失败返回空字符串
     */
    private static String fetchContentFromUrl(String urlString) throws IOException { // Renamed m6
        HttpURLConnection httpURLConnection = null;
        BufferedReader bufferedReader = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(urlString).openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setReadTimeout(20000); // 20秒超时
            httpURLConnection.setConnectTimeout(20000); // 20秒超时
            httpURLConnection.setInstanceFollowRedirects(false); // 不自动处理重定向
            // 设置固定的 User-Agent
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7");

            if (httpURLConnection.getResponseCode() == 200) { // 请求成功
                bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
                StringBuilder stringBuffer = new StringBuilder();
                String readLine;
                while ((readLine = bufferedReader.readLine()) != null) {
                    stringBuffer.append(readLine);
                }
                return stringBuffer.toString();
            } else {
                Log.w(TAG, "HTTP 请求失败，状态码: " + httpURLConnection.getResponseCode() + " URL: " + urlString);
                return ""; // 请求失败返回空
            }
        } finally {
            if (bufferedReader != null) {
                try { bufferedReader.close(); } catch (IOException ignored) {}
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    /**
     * 处理对话框按钮点击事件
     * @param dialogInterface 对话框接口
     * @param i 被点击的按钮 (POSITIVE, NEGATIVE, NEUTRAL)
     */
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        // 获取按钮对应的配置值 (可能是 URL 或特殊指令)
        String positiveAction = (C0001.positiveButtonAction != null) ? C0001.positiveButtonAction.trim() : "";
        String negativeAction = (C0001.negativeButtonAction != null) ? C0001.negativeButtonAction.trim() : "";
        String neutralAction = (C0001.neutralButtonAction != null) ? C0001.neutralButtonAction.trim() : "";

        String actionToProcess = "";
        switch (i) {
            case DialogInterface.BUTTON_POSITIVE:
                actionToProcess = positiveAction;
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                actionToProcess = negativeAction;
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                actionToProcess = neutralAction;
                break;
            default:
                Log.w(TAG, "未知的按钮点击: " + i);
                return; // 未知按钮
        }

        // 根据动作字符串执行操作
        try {
            if (actionToProcess.startsWith(decryptString("ZGr1WYZWKxy1f/1Csjl30A=="))) { // "close" 指令?
                // 阻止对话框关闭
                Field declaredField = dialogInterface.getClass().getSuperclass().getDeclaredField("mShowing");
                declaredField.setAccessible(true);
                declaredField.set(dialogInterface, Boolean.FALSE); // 阻止关闭
            } else if (actionToProcess.startsWith(decryptString("IBxPZUTTYVDzaT/o7+RBgQ=="))) { // "exit" 指令?
                currentActivity.finish(); // 关闭当前 Activity
                Process.killProcess(Process.myPid()); // 杀掉当前进程
                System.exit(0); // 退出应用
            } else if (actionToProcess.startsWith(decryptString("NgJerjRix2BLCFv3oVoFbg=="))) { // "ignore" 指令?
                // 如果包含 Toast 消息
                if (actionToProcess.contains(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) {
                    Toast.makeText(currentActivity, actionToProcess.substring(actionToProcess.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")) + 2), Toast.LENGTH_LONG).show();
                }
                // 保存版本号到 SharedPreferences (假设 neutralButtonAction 是版本号)
                if (prefs != null && C0001.neutralButtonAction != null) {
                    prefs.edit().putString("dialogVer", C0001.neutralButtonAction).apply();
                }
                // 阻止对话框关闭
                Field declaredField = dialogInterface.getClass().getSuperclass().getDeclaredField("mShowing");
                declaredField.setAccessible(true);
                declaredField.set(dialogInterface, Boolean.FALSE); // 阻止关闭
            } else if (actionToProcess.startsWith(decryptString("EYMk2mRaRH8BSPyJLiFK8A=="))) { // "http" 或 "https"
                String urlAction = actionToProcess;
                // 如果包含 Toast 消息
                if (actionToProcess.contains(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) {
                    Toast.makeText(currentActivity, actionToProcess.substring(actionToProcess.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")) + 2), Toast.LENGTH_LONG).show();
                    urlAction = actionToProcess.substring(0, actionToProcess.indexOf(decryptString("aBK2OxI211VfDrrmSw3Pfg==")));
                }
                // 提取 URL (处理可能的 HTML <a> 标签)
                String urlToOpen;
                if (urlAction.contains("href=")) {
                    int hrefStart = urlAction.indexOf("href=\"") + 6;
                    int hrefEnd = urlAction.indexOf("\"", hrefStart);
                    if (hrefStart >= 6 && hrefEnd > hrefStart) {
                         urlToOpen = urlAction.substring(hrefStart, hrefEnd);
                    } else {
                         int tagStart = urlAction.lastIndexOf("\">") + 2;
                         int tagEnd = urlAction.lastIndexOf("</a>");
                         if (tagStart >= 2 && tagEnd > tagStart) {
                              urlToOpen = urlAction.substring(tagStart, tagEnd);
                         } else {
                              urlToOpen = urlAction.substring(2); // 默认去掉前两位
                         }
                    }
                } else {
                    urlToOpen = urlAction.substring(2); // 假设前两位是协议标识
                }
                // 打开 URL
                currentActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(urlToOpen)));
            } else if (actionToProcess.startsWith(decryptString("aBK2OxI211VfDrrmSw3Pfg=="))) { // "//" Toast 消息?
                Toast.makeText(currentActivity, actionToProcess.substring(2), Toast.LENGTH_LONG).show();
            } else if (actionToProcess.startsWith("QQ") || actionToProcess.startsWith("qq")) { // QQ 相关操作
                handleQQAction(actionToProcess);
            } else {
                // 默认行为：关闭对话框
                 dialogInterface.dismiss();
            }
        } catch (Exception e) {
             Log.e(TAG, "处理按钮点击时出错: " + actionToProcess, e);
             Toast.makeText(currentActivity, "操作失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * AsyncTask 完成后在主线程执行：显示对话框
     * @param config 解析后的配置对象 (C0001 instance)
     */
    @Override
    public void onPostExecute(C0001 config) {
        super.onPostExecute(config);

        if (config == null || "关".equals(config.switchStatus)) { // 使用重命名后的变量
            Log.d(TAG, "配置为空或开关为关，不显示对话框");
            return;
        }

        // 检查版本号是否匹配 (使用重命名后的变量)
        String savedDialogVer = prefs.getString("dialogVer", "");
        if ("开".equals(config.size) && config.neutralButtonAction != null && config.neutralButtonAction.equals(savedDialogVer)) {
             Log.d(TAG, "版本号匹配，不显示对话框. 当前版本: " + config.neutralButtonAction + ", 已保存版本: " + savedDialogVer);
             return;
        }

        String dialogStyle = config.backgroundType;
        Activity activity = currentActivity;
        if (activity == null || activity.isFinishing()) {
             Log.w(TAG, "Activity 为空或正在结束，无法显示对话框");
             return;
        }

        AlertDialog.Builder builder;
        // 根据背景类型选择对话框主题
        try {
            if (dialogStyle.equals(decryptString("pXT6My4gTj82NxGYm3VW2w=="))) { // "Holo.Light"?
                 builder = new AlertDialog.Builder(activity, android.R.style.Theme_Holo_Light_Dialog);
            } else if (dialogStyle.equals(decryptString("qqYqjpPJDuzNkTREGtjyLw=="))) { // "Holo.Dark"?
                 builder = new AlertDialog.Builder(activity, android.R.style.Theme_Holo_Dialog);
            } else if (dialogStyle.equalsIgnoreCase(decryptString("uH67aS+UuGLYpIqLEsFD/g=="))) { // "DeviceDefault.Light"?
                 builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Light_Dialog);
            } else if (dialogStyle.equalsIgnoreCase(decryptString("ExuTjIN13KU3QHCSgVZ5Pw=="))) { // "DeviceDefault.Dark"?
                 builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog);
            } else if (dialogStyle.equals(decryptString("cgNoU3WYHfLgHQk8tA2CzQ=="))) { // "Traditional"?
                 builder = new AlertDialog.Builder(activity, android.R.style.Theme_Translucent_NoTitleBar);
            } else {
                 builder = new AlertDialog.Builder(activity); // 默认主题
            }
        } catch (Exception e) {
             Log.w(TAG, "无法解析对话框样式，使用默认主题", e);
             builder = new AlertDialog.Builder(activity);
        }


        // 设置标题和消息 (支持 HTML)
        try {
             builder.setTitle(Html.fromHtml(config.title)).setMessage(Html.fromHtml(config.message));
        } catch (Exception e) {
             Log.e(TAG, "设置标题或消息时出错", e);
             builder.setTitle(config.title).setMessage(config.message); // Fallback to plain text
        }

        // 根据配置添加按钮
        if (config.hasPositiveButton()) {
            builder.setPositiveButton(config.getPositiveButtonText(), this);
        }
        if (config.hasNegativeButton()) {
            builder.setNegativeButton(config.getNegativeButtonText(), this);
        }
        if (config.hasNeutralButton()) {
            builder.setNeutralButton(config.getNeutralButtonText(), this);
        }

        AlertDialog create = builder.create();

        // 设置是否点击外部可取消
        if ("关".equals(config.cancelableOutsideStr)) {
            create.setCanceledOnTouchOutside(false);
        }

        // 设置背景灰度 (Dim Amount)
        if ("关".equals(config.autoCloseTimeStr)) { // 假设 autoCloseTimeStr 控制 Dim
            try {
                 if (create.getWindow() != null) {
                      create.getWindow().setDimAmount(0.0f); // 0.0f 表示不变暗
                 }
            } catch (Exception e) {
                 Log.w(TAG, "设置 Dim Amount 失败", e);
            }
        }

        create.show();

        // 如果设置了 "阻止关闭" (假设是 switchStatus)，则通过反射修改 mShowing 标志
        if ("开".equals(config.switchStatus)) {
            try {
                Field declaredField = AlertDialog.class.getDeclaredField("mShowing");
                declaredField.setAccessible(true);
                declaredField.set(create, Boolean.FALSE); // 设置为 false 以阻止关闭
            } catch (Exception e) {
                Log.e(TAG, "阻止对话框关闭失败", e);
                publishProgress(e);
            }
        }

        // 通过反射设置标题和消息颜色
        try {
            Field declaredField2 = AlertDialog.class.getDeclaredField("mAlert");
            declaredField2.setAccessible(true);
            Object alertController = declaredField2.get(create);
            Field declaredField3 = alertController.getClass().getDeclaredField("mTitleView");
            declaredField3.setAccessible(true);
            TextView titleView = (TextView) declaredField3.get(alertController);
            if (titleView != null) {
                titleView.setTextColor(Color.parseColor(config.getTitleColor()));
            }
            Field declaredField4 = alertController.getClass().getDeclaredField("mMessageView");
            declaredField4.setAccessible(true);
            TextView messageView = (TextView) declaredField4.get(alertController);
            if (messageView != null) {
                messageView.setTextColor(Color.parseColor(config.getMessageColor()));
            }
        } catch (Exception e2) {
            Log.e(TAG, "设置标题或消息颜色失败", e2);
            publishProgress(e2);
        }

        // 设置按钮颜色
        try {
            if (config.hasPositiveButton()) {
                 create.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor(config.getPositiveButtonColor()));
            }
        } catch (Exception e3) {
            Log.e(TAG, "设置确定按钮颜色失败", e3);
            publishProgress(e3);
        }
        try {
             if (config.hasNegativeButton()) {
                 create.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.parseColor(config.getNegativeButtonColor()));
             }
        } catch (Exception e4) {
            Log.e(TAG, "设置取消按钮颜色失败", e4);
            publishProgress(e4);
        }
        try {
             if (config.hasNeutralButton()) {
                 create.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.parseColor(config.getNeutralButtonColor()));
             }
        } catch (Exception e5) {
            Log.e(TAG, "设置中性按钮颜色失败", e5);
            publishProgress(e5);
        }
    }

    @Override // android.os.AsyncTask
    protected void onPreExecute() {
        super.onPreExecute();
    }

    /**
     * 处理后台任务中的异常 (在主线程执行)
     * @param excArr 异常数组
     */
    @Override
    public void onProgressUpdate(Exception... excArr) {
        Exception[] excArr2 = excArr;
        super.onProgressUpdate(excArr2);
        if (excArr2 == null || excArr2.length == 0) return;

        Exception exc = excArr2[0];
        if (exc == null) return;

        String stackTraceString = Log.getStackTraceString(exc);
        // 如果启用了加密模式 (isEncryptedMode)，并且有异常信息，则显示一个包含错误堆栈的对话框
        if (!isEncryptedMode || stackTraceString == null || stackTraceString.isEmpty()) {
            return;
        }
        try {
            new AlertDialog.Builder(currentActivity)
                    .setTitle(decryptString("DMDohhFHQ94eW0MXxQBNmQ==")) // 可能解密为 "错误"
                    .setMessage(stackTraceString)
                    .setPositiveButton("复制", new DialogInterfaceOnClickListenerC0000(this, stackTraceString)) // 复制按钮
                    .setNegativeButton("取消", (DialogInterface.OnClickListener) null) // 取消按钮
                    .create().show();
        } catch (Exception e) {
             Log.e(TAG, "显示错误对话框失败", e);
        }
    }
}
