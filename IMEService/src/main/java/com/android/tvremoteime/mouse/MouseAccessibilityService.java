package com.android.tvremoteime.mouse;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * 辅助功能服务 - 用于模拟鼠标/触控板操作
 * 替代原来的ADB方案，更简单稳定
 */
public class MouseAccessibilityService extends AccessibilityService {
    private static final String TAG = "MouseAccessibility";

    private static MouseAccessibilityService instance;
    private MouseCursorOverlay cursorOverlay;
    private Handler mainHandler;

    // 鼠标位置
    private int mouseX;
    private int mouseY;
    private int screenWidth;
    private int screenHeight;

    public static MouseAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isServiceEnabled() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MouseAccessibilityService created");
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.i(TAG, "MouseAccessibilityService connected");

        initScreenSize();
        initCursorOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理辅助功能事件
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "MouseAccessibilityService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (cursorOverlay != null) {
            cursorOverlay.hide();
            cursorOverlay = null;
        }
        Log.i(TAG, "MouseAccessibilityService destroyed");
    }

    private void initScreenSize() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        // 初始位置为屏幕中心
        mouseX = screenWidth / 2;
        mouseY = screenHeight / 2;
        Log.i(TAG, "Screen size: " + screenWidth + "x" + screenHeight);
    }

    private void initCursorOverlay() {
        mainHandler.post(() -> {
            cursorOverlay = new MouseCursorOverlay(this);
            cursorOverlay.show();
            cursorOverlay.updatePosition(mouseX, mouseY);
        });
    }

    /**
     * 显示鼠标光标
     */
    public void showCursor() {
        mainHandler.post(() -> {
            if (cursorOverlay != null) {
                cursorOverlay.show();
            }
        });
    }

    /**
     * 隐藏鼠标光标
     */
    public void hideCursor() {
        mainHandler.post(() -> {
            if (cursorOverlay != null) {
                cursorOverlay.hide();
            }
        });
    }

    /**
     * 移动鼠标
     * @param dx X方向移动距离
     * @param dy Y方向移动距离
     * @return 新的鼠标位置 [x, y]
     */
    public int[] moveMouse(int dx, int dy) {
        mouseX = Math.max(0, Math.min(screenWidth - 1, mouseX + dx));
        mouseY = Math.max(0, Math.min(screenHeight - 1, mouseY + dy));

        mainHandler.post(() -> {
            if (cursorOverlay != null) {
                cursorOverlay.updatePosition(mouseX, mouseY);
            }
        });

        return new int[]{mouseX, mouseY};
    }

    /**
     * 鼠标点击
     * @param button 按钮 (0=左键, 1=右键/长按, 2=中键)
     * @return 是否成功
     */
    public boolean click(int button) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "dispatchGesture requires API 24+");
            return false;
        }

        if (button == 1) {
            // 右键 - 长按
            return performLongClick(mouseX, mouseY);
        } else {
            // 左键或中键 - 普通点击
            return performClick(mouseX, mouseY);
        }
    }

    /**
     * 鼠标滚动
     * @param dy 滚动距离 (正数向下，负数向上)
     * @return 是否成功
     */
    public boolean scroll(int dy) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "dispatchGesture requires API 24+");
            return false;
        }

        int scrollAmount = dy * 50; // 放大滚动效果
        int startY = mouseY;
        int endY = Math.max(0, Math.min(screenHeight - 1, mouseY + scrollAmount));

        return performSwipe(mouseX, startY, mouseX, endY, 200);
    }

    /**
     * 执行点击手势
     */
    private boolean performClick(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(clickPath, 0, 50);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Click completed at " + x + "," + y);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Click cancelled at " + x + "," + y);
            }
        }, null);
    }

    /**
     * 执行长按手势
     */
    private boolean performLongClick(int x, int y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(clickPath, 0, 600); // 600ms长按

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Long click completed at " + x + "," + y);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Long click cancelled at " + x + "," + y);
            }
        }, null);
    }

    /**
     * 执行滑动手势
     */
    private boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        Path swipePath = new Path();
        swipePath.moveTo(startX, startY);
        swipePath.lineTo(endX, endY);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(swipePath, 0, duration);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Swipe completed");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Swipe cancelled");
            }
        }, null);
    }

    /**
     * 获取当前鼠标位置
     */
    public int[] getMousePosition() {
        return new int[]{mouseX, mouseY};
    }

    /**
     * 重置鼠标位置到屏幕中心
     */
    public void resetMousePosition() {
        mouseX = screenWidth / 2;
        mouseY = screenHeight / 2;
        mainHandler.post(() -> {
            if (cursorOverlay != null) {
                cursorOverlay.updatePosition(mouseX, mouseY);
            }
        });
    }
}
