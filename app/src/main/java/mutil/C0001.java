package mutil;

import android.util.Log;

/**
 * 配置解析类 (可能来自反编译)
 * 用于解析特定格式的配置字符串
 */
final class C0001 {

    // 原始配置字符串内容
    static String f6;

    // 确定按钮相关配置 (可能指URL或动作)
    static String f7;

    // 取消按钮相关配置 (可能指URL或动作)
    static String f8;

    // 中性按钮相关配置 (可能指URL或动作)
    static String f9;

    // 背景颜色 (未使用?)
    static String f10;

    // 背景图片 URL
    static String f11;

    // 确定按钮颜色
    String f12;

    // 是否显示确定按钮 ("是"/"否")
    String f13;

    // 是否显示取消按钮 ("是"/"否")
    String f14;

    // 标题颜色
    String f15;

    // 标题文本
    String f16;

    // 消息颜色
    String f17;

    // 消息文本
    String f18;

    // 是否点击外部可关闭 ("是"/"否")
    String f19;

    // 确定按钮文本
    String f20;

    // 对话框形状 (如 "矩形")
    String f21;

    // 中性按钮文本
    String f22;

    // 取消按钮颜色
    String f23;

    // 对话框尺寸 (如 "中")
    String f24;

    // 取消按钮文本
    String f25;

    // 背景类型 (如 "纯色")
    String f26;

    // 对话框总开关 ("开"/"关")
    String f27;

    // 是否显示中性按钮 ("是"/"否")
    String f28;

    // 自动关闭时间 (或是否自动关闭 "是"/"否")
    String f29;

    // 中性按钮颜色
    String f30;

    C0001(String content) {
        f6 = content;
        parseContent();
    }

    private void parseContent() {
        try {
            // 检查内容是否为空
            if (f6 == null || f6.isEmpty()) {
                Log.e("C0001", "配置内容为空");
                return;
            }

            // 解析各配置项的值
            f16 = m5("标题");
            f18 = m5("消息");
            f15 = m5("标题颜色");
            f17 = m5("消息颜色");
            f20 = m5("确定按钮文本");
            f25 = m5("取消按钮文本");
            f22 = m5("中性按钮文本");
            f12 = m5("确定按钮颜色");
            f23 = m5("取消按钮颜色");
            f30 = m5("中性按钮颜色");
            f13 = m5("显示确定按钮");
            f14 = m5("显示取消按钮");
            f28 = m5("显示中性按钮");
            f19 = m5("点击外部关闭");
            f29 = m5("自动关闭时间");
            f11 = m5("背景图片");
            f8 = m5("取消按钮");
            f7 = m5("确定按钮");
            f26 = m5("背景类型");
            f27 = m5("开关");
            f21 = m5("形状");
            f24 = m5("尺寸");

            // 为空的配置项设置默认值
            if (f16 == null) f16 = "提示";
            if (f18 == null) f18 = "暂无消息";
            if (f15 == null) f15 = "#000000";
            if (f17 == null) f17 = "#000000";
            if (f20 == null) f20 = "确定";
            if (f25 == null) f25 = "取消";
            if (f22 == null) f22 = "";
            if (f12 == null) f12 = "#000000";
            if (f23 == null) f23 = "#000000";
            if (f30 == null) f30 = "#000000";
            if (f13 == null) f13 = "是";
            if (f14 == null) f14 = "是";
            if (f28 == null) f28 = "否";
            if (f19 == null) f19 = "否";
            if (f29 == null) f29 = "否";
            if (f11 == null) f11 = "";
            if (f8 == null) f8 = "";
            if (f7 == null) f7 = "";
            if (f26 == null) f26 = "纯色";
            if (f27 == null) f27 = "开";
            if (f21 == null) f21 = "矩形";
            if (f24 == null) f24 = "中";
        } catch (Exception e) {
            Log.e("C0001", "解析配置出错", e);
        }
    }

    // 从配置字符串中提取指定键的值 (使用特定标记 "〈key〉...〈/key〉")
    private String m5(String key) {
        if (key != null && !key.trim().isEmpty() && f6 != null && !f6.isEmpty()) {
            try {
                String startTag = "〈" + key + "〉";
                String endTag = "〈/" + key + "〉";
                String configString = f6;

                int startIndex = configString.indexOf(startTag);
                int endIndex = configString.indexOf(endTag); // Use configString here too

                if (startIndex >= 0 && endIndex > startIndex) {
                    return configString.substring(startIndex + startTag.length(), endIndex);
                } else {
                    Log.w("C0001", "未找到配置项：" + key);
                    return "";
                }
            } catch (Exception e) {
                Log.e("C0001", "获取配置 " + key + " 失败", e);
                return "";
            }
        }
        return "";
    }

    public String getTitle() {
        return f16;
    }

    public String getMessage() {
        return f18;
    }

    public String getTitleColor() {
        return f15;
    }

    public String getMessageColor() {
        return f17;
    }

    public String getPositiveButtonText() {
        return f20;
    }

    public String getNegativeButtonText() {
        return f25;
    }

    public String getNeutralButtonText() {
        return f22;
    }

    public String getPositiveButtonColor() {
        return f12;
    }

    public String getNegativeButtonColor() {
        return f23;
    }

    public String getNeutralButtonColor() {
        return f30;
    }

    public boolean hasPositiveButton() {
        return !"否".equals(f13);
    }

    public boolean hasNegativeButton() {
        return !"否".equals(f14);
    }

    public boolean hasNeutralButton() {
        return !"否".equals(f28);
    }
}
