package com.android.tvremoteime.server;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.tvremoteime.mouse.MouseAccessibilityService;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * 处理鼠标/触摸板相关的 HTTP 请求
 * 使用辅助功能服务实现，无需ADB连接
 *
 * 端点:
 * - POST /mouse/move   - 相对移动鼠标 (参数: dx, dy)
 * - POST /mouse/click  - 鼠标点击 (参数: button - 0=左键, 1=右键, 2=中键)
 * - POST /mouse/scroll - 滚轮滚动 (参数: dy)
 * - GET  /mouse/status - 获取鼠标状态和辅助功能服务状态
 * - POST /mouse/show   - 显示鼠标光标
 * - POST /mouse/hide   - 隐藏鼠标光标
 */
public class MouseRequestProcesser implements RequestProcesser {
    private static final String TAG = "MouseRequestProcesser";
    private Context context;
    private RemoteServer remoteServer;

    public MouseRequestProcesser(Context context, RemoteServer remoteServer) {
        this.context = context;
        this.remoteServer = remoteServer;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            switch (fileName) {
                case "/mouse/move":
                case "/mouse/click":
                case "/mouse/scroll":
                case "/mouse/show":
                case "/mouse/hide":
                    return true;
            }
        } else if (session.getMethod() == NanoHTTPD.Method.GET) {
            if ("/mouse/status".equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName,
                                          Map<String, String> params, Map<String, String> files) {
        // 处理状态查询
        if ("/mouse/status".equals(fileName)) {
            return handleStatus();
        }

        // 检查辅助功能服务是否已启用
        MouseAccessibilityService service = MouseAccessibilityService.getInstance();
        if (service == null) {
            String errorMsg = "error:accessibility_not_enabled";
            Log.w(TAG, "MouseAccessibilityService not enabled");
            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                    "{\"status\":\"error\",\"message\":\"请先在系统设置中启用辅助功能服务\",\"code\":\"accessibility_not_enabled\"}");
        }

        switch (fileName) {
            case "/mouse/move":
                return handleMouseMove(params, service);
            case "/mouse/click":
                return handleMouseClick(params, service);
            case "/mouse/scroll":
                return handleMouseScroll(params, service);
            case "/mouse/show":
                return handleShowCursor(service);
            case "/mouse/hide":
                return handleHideCursor(service);
            default:
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND,
                        "Error 404, file not found.");
        }
    }

    /**
     * 获取鼠标和辅助功能状态
     */
    private NanoHTTPD.Response handleStatus() {
        boolean serviceEnabled = MouseAccessibilityService.isServiceEnabled();
        MouseAccessibilityService service = MouseAccessibilityService.getInstance();

        int mouseX = 0, mouseY = 0;
        if (service != null) {
            int[] pos = service.getMousePosition();
            mouseX = pos[0];
            mouseY = pos[1];
        }

        // 获取屏幕尺寸
        int screenWidth = 0, screenHeight = 0;
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get screen size", e);
        }

        String json = String.format(
            "{\"serviceEnabled\":%b,\"mouseX\":%d,\"mouseY\":%d,\"screenWidth\":%d,\"screenHeight\":%d,\"apiLevel\":%d}",
            serviceEnabled, mouseX, mouseY, screenWidth, screenHeight, Build.VERSION.SDK_INT
        );

        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, json);
    }

    /**
     * 处理鼠标移动
     * 参数: dx, dy (相对移动距离)
     */
    private NanoHTTPD.Response handleMouseMove(Map<String, String> params, MouseAccessibilityService service) {
        try {
            int dx = Integer.parseInt(params.getOrDefault("dx", "0"));
            int dy = Integer.parseInt(params.getOrDefault("dy", "0"));

            int[] newPos = service.moveMouse(dx, dy);

            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                    String.format("{\"status\":\"ok\",\"x\":%d,\"y\":%d}", newPos[0], newPos[1]));
        } catch (NumberFormatException e) {
            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                    "{\"status\":\"error\",\"message\":\"invalid params\"}");
        }
    }

    /**
     * 处理鼠标点击
     * 参数: button (0=左键, 1=右键, 2=中键)
     */
    private NanoHTTPD.Response handleMouseClick(Map<String, String> params, MouseAccessibilityService service) {
        try {
            int button = Integer.parseInt(params.getOrDefault("button", "0"));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"error\",\"message\":\"需要Android 7.0或更高版本\"}");
            }

            boolean success = service.click(button);

            if (success) {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"ok\"}");
            } else {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"error\",\"message\":\"click failed\"}");
            }
        } catch (NumberFormatException e) {
            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                    "{\"status\":\"error\",\"message\":\"invalid params\"}");
        }
    }

    /**
     * 处理滚轮滚动
     * 参数: dy (滚动距离，正数向下，负数向上)
     */
    private NanoHTTPD.Response handleMouseScroll(Map<String, String> params, MouseAccessibilityService service) {
        try {
            int dy = Integer.parseInt(params.getOrDefault("dy", "0"));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"error\",\"message\":\"需要Android 7.0或更高版本\"}");
            }

            boolean success = service.scroll(dy);

            if (success) {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"ok\"}");
            } else {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                        "{\"status\":\"error\",\"message\":\"scroll failed\"}");
            }
        } catch (NumberFormatException e) {
            return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                    "{\"status\":\"error\",\"message\":\"invalid params\"}");
        }
    }

    /**
     * 显示鼠标光标
     */
    private NanoHTTPD.Response handleShowCursor(MouseAccessibilityService service) {
        service.showCursor();
        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                "{\"status\":\"ok\"}");
    }

    /**
     * 隐藏鼠标光标
     */
    private NanoHTTPD.Response handleHideCursor(MouseAccessibilityService service) {
        service.hideCursor();
        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK,
                "{\"status\":\"ok\"}");
    }
}
