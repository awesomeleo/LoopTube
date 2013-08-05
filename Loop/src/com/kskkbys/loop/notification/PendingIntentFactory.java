package com.kskkbys.loop.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.ui.MainActivity;

/**
 * This class creates PendingIntent objects for notification and widget.
 * @author keisuke.kobayashi
 *
 */
public class PendingIntentFactory {
	
	private static final int REQUEST_CODE_LAUNCH = 1000;
	private static final int REQUEST_CODE_PREV = 1001;
	private static final int REQUEST_CODE_PAUSE = 1002;
	private static final int REQUEST_CODE_PLAY = 1003;
	private static final int REQUEST_CODE_NEXT = 1004;
	
	public static PendingIntent getLaunchIntent(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(MainActivity.FROM_NOTIFICATION, true);
		PendingIntent contentIntent = PendingIntent.getActivity(context, REQUEST_CODE_LAUNCH,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return contentIntent;
	}
	
	public static PendingIntent getPrevIntent(Context context) {
		Intent prevIntent = new Intent(context, VideoPlayerService.class);
		prevIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PREV);
		PendingIntent prevPendingIntent = PendingIntent.getService(context, REQUEST_CODE_PREV, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return prevPendingIntent;
	}

	public static PendingIntent getPauseIntent(Context context) {
		Intent pauseIntent = new Intent(context, VideoPlayerService.class);
		pauseIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PAUSE);
		PendingIntent pausePendingIntent = PendingIntent.getService(context, REQUEST_CODE_PAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return pausePendingIntent;
	}
	
	public static PendingIntent getPlayIntent(Context context) {
		Intent playIntent = new Intent(context, VideoPlayerService.class);
		playIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PLAY);
		PendingIntent playPendingIntent = PendingIntent.getService(context, REQUEST_CODE_PLAY, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return playPendingIntent;
	}

	public static PendingIntent getNextIntent(Context context) {
		Intent nextIntent = new Intent(context, VideoPlayerService.class);
		nextIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_NEXT);
		PendingIntent nextPendingIntent = PendingIntent.getService(context, REQUEST_CODE_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return nextPendingIntent;
	}

}
