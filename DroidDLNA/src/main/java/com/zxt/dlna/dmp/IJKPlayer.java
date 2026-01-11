package com.zxt.dlna.dmp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Simplified video player that uses system player.
 * Replaces IJKPlayer after removing ijkplayer module.
 */
public class IJKPlayer extends Activity {

    public static GPlayer.MediaListener mMediaListener;

    public static void setMediaListener(GPlayer.MediaListener mediaListener) {
        mMediaListener = mediaListener;
    }

    public static void intentTo(Class<?> cls, Context context, String videoPath, String videoTitle) {
        intentTo(cls, context, videoPath, videoTitle, 0);
    }

    public static void intentTo(Class<?> cls, Context context, String videoPath, String videoTitle, int videoIndex) {
        // Use system video player
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoPath), "video/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String videoPath = getIntent().getStringExtra("videoPath");
        String videoTitle = getIntent().getStringExtra("videoTitle");

        if (videoPath != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(videoPath), "video/*");
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        mMediaListener = null;
        super.onDestroy();
    }
}
