package com.android.tvremoteime.mouse;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.tvremoteime.R;

/**
 * 鼠标光标悬浮窗
 * 在屏幕上显示一个可移动的鼠标光标图标
 */
public class MouseCursorOverlay {
    private static final String TAG = "MouseCursorOverlay";

    private Context context;
    private WindowManager windowManager;
    private View cursorView;
    private WindowManager.LayoutParams layoutParams;
    private boolean isShowing = false;

    // 光标图标的热点偏移（光标尖端相对于图标左上角的位置）
    private int hotspotX = 0;
    private int hotspotY = 0;

    public MouseCursorOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        initCursorView();
    }

    private void initCursorView() {
        // 创建光标视图
        cursorView = new ImageView(context);
        ((ImageView) cursorView).setImageResource(R.drawable.mouse_cursor);

        // 设置窗口参数
        int windowType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            windowType = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else {
            windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        layoutParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 0;
    }

    /**
     * 显示光标
     */
    public void show() {
        if (!isShowing && cursorView != null) {
            try {
                windowManager.addView(cursorView, layoutParams);
                isShowing = true;
                Log.i(TAG, "Cursor overlay shown");
            } catch (Exception e) {
                Log.e(TAG, "Failed to show cursor overlay", e);
            }
        }
    }

    /**
     * 隐藏光标
     */
    public void hide() {
        if (isShowing && cursorView != null) {
            try {
                windowManager.removeView(cursorView);
                isShowing = false;
                Log.i(TAG, "Cursor overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Failed to hide cursor overlay", e);
            }
        }
    }

    /**
     * 更新光标位置
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     */
    public void updatePosition(int x, int y) {
        if (isShowing && cursorView != null) {
            layoutParams.x = x - hotspotX;
            layoutParams.y = y - hotspotY;
            try {
                windowManager.updateViewLayout(cursorView, layoutParams);
            } catch (Exception e) {
                Log.e(TAG, "Failed to update cursor position", e);
            }
        }
    }

    /**
     * 设置光标热点偏移
     * @param x 热点X偏移
     * @param y 热点Y偏移
     */
    public void setHotspot(int x, int y) {
        this.hotspotX = x;
        this.hotspotY = y;
    }

    /**
     * 检查光标是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }
}
