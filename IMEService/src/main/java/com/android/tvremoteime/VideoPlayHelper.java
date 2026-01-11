package com.android.tvremoteime;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Helper class for video playback.
 * Simplified version - uses system video player only.
 */
public class VideoPlayHelper {

    /**
     * Play video URL using system player.
     *
     * @param context Android context
     * @param url Video URL to play
     * @param videoIndex Video index (unused in simplified version)
     * @param useSystem Whether to use system player (always true in simplified version)
     */
    public static void playUrl(Context context, String url, int videoIndex, boolean useSystem) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (Environment.needDebug) {
            Environment.debug(IMEService.TAG, "Playing video with system player, url=" + url);
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            if (Environment.needDebug) {
                Environment.debug(IMEService.TAG, "Failed to play video: " + e.getMessage());
            }
        }
    }
}
