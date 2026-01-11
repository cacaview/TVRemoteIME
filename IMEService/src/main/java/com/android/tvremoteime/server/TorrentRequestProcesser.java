package com.android.tvremoteime.server;

import android.content.Context;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Torrent request processor.
 * Note: Torrent/Thunder download functionality has been removed in v2.0.
 * This processor returns a disabled message for backward compatibility.
 */
public class TorrentRequestProcesser implements RequestProcesser {
    private Context context;

    public TorrentRequestProcesser(Context context) {
        this.context = context;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        if (session.getMethod() == NanoHTTPD.Method.POST) {
            switch (fileName) {
                case "/torrent/data":
                case "/torrent/upload":
                case "/torrent/play":
                    return true;
            }
        }
        return "/torrent".equalsIgnoreCase(fileName);
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName,
                                          Map<String, String> params, Map<String, String> files) {
        // Return disabled message - torrent functionality removed in v2.0
        return RemoteServer.createJSONResponse(
            NanoHTTPD.Response.Status.OK,
            "{\"success\":false,\"message\":\"Torrent functionality has been removed in v2.0\"}"
        );
    }
}
