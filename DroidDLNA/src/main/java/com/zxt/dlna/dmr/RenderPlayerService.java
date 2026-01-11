/*
 * RenderPlayerService.java
 * Description: Service for DLNA rendering
 * Simplified version using system player
 */

package com.zxt.dlna.dmr;

import com.zxt.dlna.dmp.IJKPlayer;
import com.zxt.dlna.util.Action;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

public class RenderPlayerService extends Service {

	public IBinder onBind(Intent intent) {
		return null;
	}

	public void onStart(Intent intent, int startId) {
		if (null != intent) {
			super.onStart(intent, startId);
			String type = intent.getStringExtra("type");

			if (type != null && (type.equals("audio") || type.equals("video"))) {
				// Use system player
				String playURI = intent.getStringExtra("playURI");
				if (playURI != null) {
					try {
						Intent playIntent = new Intent(Intent.ACTION_VIEW);
						playIntent.setDataAndType(Uri.parse(playURI), "video/*");
						playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(playIntent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				Intent intent2 = new Intent(Action.DMR);
				intent2.putExtra("playpath", intent.getStringExtra("playURI"));
				sendBroadcast(intent2);
			}
		}
	}
}
