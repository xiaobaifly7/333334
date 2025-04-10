package mutil;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context; // Import Context
import android.content.DialogInterface;

/**
 * 对话框按钮点击监听器 (可能来自反编译)
 * 用于处理特定按钮点击事件，将文本复制到剪贴板。
 */
final class DialogInterfaceOnClickListenerC0000 implements DialogInterface.OnClickListener {

    // 要复制到剪贴板的内容
    final /* synthetic */ String f4; // Restored from contentToCopy

    // 关联的 OnlineDialog 实例
    final /* synthetic */ OnlineDialog f5; // Restored from onlineDialogInstance

    DialogInterfaceOnClickListenerC0000(OnlineDialog onlineDialog, String content) { // Renamed str to content
        this.f5 = onlineDialog; // Restored from onlineDialogInstance
        this.f4 = content; // Restored from contentToCopy, str
    }

    @Override // android.content.DialogInterface.OnClickListener
    public final void onClick(DialogInterface dialogInterface, int i) {
        try {
            Activity activity = OnlineDialog.currentActivity; // Use renamed static field from OnlineDialog
            if (activity != null) { // Add null check for activity
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE); // Use Context.CLIPBOARD_SERVICE
                if (clipboard != null) {
                    clipboard.setText(this.f4); // Restored from contentToCopy
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
