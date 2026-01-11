package com.android.tvremoteime.server;

import android.content.Context;
import android.text.TextUtils;

import com.android.tvremoteime.VideoPlayHelper;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Handles video playback requests.
 * Simplified version - uses system player only.
 */
public class PlayRequestProcesser implements RequestProcesser {
    private Context context;

    // Fast forward interval in milliseconds (default 5 seconds)
    private static int fastForwardInterval = 5000;

    public PlayRequestProcesser(Context context) {
        this.context = context;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            switch (fileName) {
                case "/play":
                case "/playStop":
                case "/changePlayFFI":
                    return true;
            }
        }
        return false;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName,
                                          Map<String, String> params, Map<String, String> files) {
        switch (fileName) {
            case "/play":
                if (!TextUtils.isEmpty(params.get("playUrl"))) {
                    VideoPlayHelper.playUrl(this.context, params.get("playUrl"), 0,
                        "true".equalsIgnoreCase(params.get("useSystem")));
                }
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");

            case "/playStop":
                // Note: Stop functionality removed - system player handles its own lifecycle
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");

            case "/changePlayFFI":
                if (!TextUtils.isEmpty(params.get("speedInterval"))) {
                    try {
                        fastForwardInterval = Integer.parseInt(params.get("speedInterval")) * 1000;
                    } catch (NumberFormatException ignored) {
                    }
                }
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");

            default:
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND,
                    "Error 404, file not found.");
        }
    }

    public static int getFastForwardInterval() {
        return fastForwardInterval;
    }
}
