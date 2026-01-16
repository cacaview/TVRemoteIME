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

import com.android.tvremoteime.adb.AdbHelper;

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

    // 自动隐藏光标相关
    private static final long CURSOR_HIDE_DELAY = 5000; // 5秒不动就隐藏
    private Runnable hideCursorRunnable;
    private boolean isCursorHidden = false;

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

        // 初始化AdbHelper作为回退方案
        if (AdbHelper.getInstance() == null) {
            AdbHelper.createInstance();
            Log.i(TAG, "AdbHelper instance created");
        }
        // 初始化ADB服务连接
        if (AdbHelper.initService(this)) {
            Log.i(TAG, "AdbHelper service initialized");
        }
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
        // 清理自动隐藏任务
        if (hideCursorRunnable != null) {
            mainHandler.removeCallbacks(hideCursorRunnable);
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

            // 初始化自动隐藏任务并启动计时器
            hideCursorRunnable = this::autoHideCursor;
            resetHideTimer(); // 启动5秒自动隐藏计时器
        });
    }

    /**
     * 自动隐藏光标
     */
    private void autoHideCursor() {
        if (cursorOverlay != null && cursorOverlay.isShowing()) {
            cursorOverlay.hide();
            isCursorHidden = true;
            Log.i(TAG, "Cursor auto-hidden due to inactivity");
        }
    }

    /**
     * 重置自动隐藏计时器
     */
    private void resetHideTimer() {
        // 取消之前的隐藏任务
        mainHandler.removeCallbacks(hideCursorRunnable);

        // 如果光标当前是隐藏的，先显示出来（必须在主线程执行）
        if (isCursorHidden && cursorOverlay != null) {
            mainHandler.post(() -> {
                cursorOverlay.show();
                cursorOverlay.updatePosition(mouseX, mouseY);
            });
            isCursorHidden = false;
        }

        // 重新安排5秒后隐藏
        mainHandler.postDelayed(hideCursorRunnable, CURSOR_HIDE_DELAY);
    }

/**
     * 显示鼠标光标
     */
    public void showCursor() {
        mainHandler.post(() -> {
            if (cursorOverlay != null) {
                cursorOverlay.show();
                resetHideTimer();
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
                isCursorHidden = true;
                // 取消自动隐藏计时器
                mainHandler.removeCallbacks(hideCursorRunnable);
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

        // 重置自动隐藏计时器
        resetHideTimer();

        return new int[]{mouseX, mouseY};
    }

/**
     * 鼠标点击
     * @param button 按钮 (0=左键, 1=右键/返回, 2=中键)
     * @return 是否成功
     */
    public boolean click(int button) {
        // 重置自动隐藏计时器
        resetHideTimer();

        if (button == 1) {
            // 右键 - 执行返回操作（符合安卓设备鼠标操作习惯）
            return performGlobalAction(GLOBAL_ACTION_BACK);
        } else {
            // 左键或中键 - 普通点击
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.e(TAG, "dispatchGesture requires API 24+");
                return false;
            }
            return performClick(mouseX, mouseY);
        }
    }

/**
     * 鼠标滚动
     * @param dy 滚动距离 (正数向下，负数向上)
     * @return 是否成功
     */
    public boolean scroll(int dy) {
        // 重置自动隐藏计时器
        resetHideTimer();

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
            return performClickViaShell(x, y);
        }

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(clickPath, 0, 50);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        final int clickX = x;
        final int clickY = y;

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Click completed at " + clickX + "," + clickY);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Click cancelled at " + clickX + "," + clickY + ", trying shell fallback");
                // 手势被取消时，尝试使用shell命令
                performClickViaShell(clickX, clickY);
            }
        }, null);
    }

    /**
     * 通过shell命令执行点击（回退方案）
     */
    private boolean performClickViaShell(int x, int y) {
        // 尝试使用AdbHelper
        AdbHelper adbHelper = AdbHelper.getInstance();
        Log.w(TAG, "performClickViaShell: AdbHelper instance: " + (adbHelper != null ? "exists" : "null"));
        if (adbHelper != null) {
            Log.w(TAG, "performClickViaShell: AdbHelper isRunning: " + adbHelper.isRunning());
        }
        if (adbHelper != null && adbHelper.isRunning()) {
            String command = String.format("shell:input tap %d %d", x, y);
            adbHelper.sendData(command);
            Log.w(TAG, "ADB click executed at " + x + "," + y);
            return true;
        }

        // 回退到Runtime.exec（可能没有权限）
        try {
            String command = String.format("input tap %d %d", x, y);
            Log.w(TAG, "Trying Runtime.exec: " + command);
            Runtime.getRuntime().exec(command);
            Log.w(TAG, "Shell click executed at " + x + "," + y);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Shell click failed", e);
            return false;
        }
    }

    /**
     * 执行长按手势
     */
    private boolean performLongClick(int x, int y, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return performLongClickViaShell(x, y, duration);
        }

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(clickPath, 0, duration);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        final int clickX = x;
        final int clickY = y;
        final int clickDuration = duration;

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Long click completed at " + clickX + "," + clickY);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Long click cancelled at " + clickX + "," + clickY + ", trying shell fallback");
                // 手势被取消时，尝试使用shell命令
                performLongClickViaShell(clickX, clickY, clickDuration);
            }
        }, null);
    }

    /**
     * 通过shell命令执行长按（回退方案）
     */
    private boolean performLongClickViaShell(int x, int y, int duration) {
        // 尝试使用AdbHelper
        AdbHelper adbHelper = AdbHelper.getInstance();
        if (adbHelper != null && adbHelper.isRunning()) {
            String command = String.format("shell:input swipe %d %d %d %d %d", x, y, x, y, duration);
            adbHelper.sendData(command);
            Log.d(TAG, "ADB long click executed at " + x + "," + y + " duration=" + duration);
            return true;
        }

        // 回退到Runtime.exec（可能没有权限）
        try {
            String command = String.format("input swipe %d %d %d %d %d", x, y, x, y, duration);
            Runtime.getRuntime().exec(command);
            Log.d(TAG, "Shell long click executed at " + x + "," + y + " duration=" + duration);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Shell long click failed", e);
            return false;
        }
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
     * 执行上划手势
     * @param distance 滑动距离（像素）
     * @return 是否成功
     */
    public boolean swipeUp(int distance) {
        // 重置自动隐藏计时器
        resetHideTimer();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return performSwipeViaShell(mouseX, mouseY, mouseX, mouseY - distance);
        }

        int startY = mouseY;
        int endY = Math.max(0, mouseY - distance);
        return performSwipeWithFallback(mouseX, startY, mouseX, endY, 200);
    }

/**
     * 执行下划手势
     * @param distance 滑动距离（像素）
     * @return 是否成功
     */
    public boolean swipeDown(int distance) {
        // 重置自动隐藏计时器
        resetHideTimer();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return performSwipeViaShell(mouseX, mouseY, mouseX, mouseY + distance);
        }

        int startY = mouseY;
        int endY = Math.min(screenHeight - 1, mouseY + distance);
        return performSwipeWithFallback(mouseX, startY, mouseX, endY, 200);
    }

/**
     * 执行长按
     * @param duration 长按时间（毫秒）
     * @return 是否成功
     */
    public boolean longClick(int duration) {
        // 重置自动隐藏计时器
        resetHideTimer();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return performLongClickViaShell(mouseX, mouseY, duration);
        }
        return performLongClick(mouseX, mouseY, duration);
    }

    /**
     * 执行滑动手势（带回退）
     */
    private boolean performSwipeWithFallback(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return performSwipeViaShell(startX, startY, endX, endY);
        }

        Path swipePath = new Path();
        swipePath.moveTo(startX, startY);
        swipePath.lineTo(endX, endY);

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(swipePath, 0, duration);

        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(stroke)
            .build();

        final int fStartX = startX, fStartY = startY, fEndX = endX, fEndY = endY;

        return dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Swipe completed from " + fStartX + "," + fStartY + " to " + fEndX + "," + fEndY);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "Swipe cancelled, trying shell fallback");
                performSwipeViaShell(fStartX, fStartY, fEndX, fEndY);
            }
        }, null);
    }

    /**
     * 通过shell命令执行滑动（回退方案）
     */
    private boolean performSwipeViaShell(int startX, int startY, int endX, int endY) {
        // 尝试使用AdbHelper
        AdbHelper adbHelper = AdbHelper.getInstance();
        if (adbHelper != null && adbHelper.isRunning()) {
            String command = String.format("shell:input swipe %d %d %d %d 200", startX, startY, endX, endY);
            adbHelper.sendData(command);
            Log.d(TAG, "ADB swipe executed from " + startX + "," + startY + " to " + endX + "," + endY);
            return true;
        }

        // 回退到Runtime.exec（可能没有权限）
        try {
            String command = String.format("input swipe %d %d %d %d 200", startX, startY, endX, endY);
            Runtime.getRuntime().exec(command);
            Log.d(TAG, "Shell swipe executed from " + startX + "," + startY + " to " + endX + "," + endY);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Shell swipe failed", e);
            return false;
        }
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
