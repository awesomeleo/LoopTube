package com.kskkbys.loop.ui.widget;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.service.VideoPlayerService.PlayerEvent;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;

/**
 * 
 * @author keisuke.kobayashi
 *
 */
public class LoopWidgetProvider extends AppWidgetProvider {

	private static final String TAG = LoopWidgetProvider.class.getSimpleName();

	private static final int REQUEST_CODE_PREV = 1;
	private static final int REQUEST_CODE_PAUSE = 2;
	private static final int REQUEST_CODE_NEXT = 3;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		KLog.v(TAG, "onUpdate");
		// Start service to receive broadcast
		Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
		context.startService(intent);
		// Update view
		updateRemoteViews(context, true, false);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		KLog.v(TAG, "onDeleted");
	}

	private static void updateRemoteViews(Context context, boolean isPlaying, boolean isLoading) {
		AppWidgetManager awm = AppWidgetManager.getInstance(context);
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		// title
		if (!isLoading) {
			Video video = Playlist.getInstance().getCurrentVideo();
			if (video != null) {
				remoteViews.setTextViewText(R.id.widget_title, video.getTitle());
			} else {
				remoteViews.setTextViewText(R.id.widget_title, context.getText(R.string.loop_widget_not_playing));
			}
		} else {
			remoteViews.setTextViewText(R.id.widget_title, context.getText(R.string.loop_widget_loading));
		}
		// next
		Intent nextIntent = new Intent(context, VideoPlayerService.class);
		nextIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_NEXT);
		PendingIntent nextPendingIntent = PendingIntent.getService(context, REQUEST_CODE_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent);
		// pause
		if (isPlaying) {
			remoteViews.setImageViewResource(R.id.widget_pause, R.drawable.pause);
			Intent pauseIntent = new Intent(context, VideoPlayerService.class);
			pauseIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PAUSE);
			PendingIntent pausePendingIntent = PendingIntent.getService(context, REQUEST_CODE_PAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_pause, pausePendingIntent);
		} else {
			remoteViews.setImageViewResource(R.id.widget_pause, R.drawable.play);
			Intent playIntent = new Intent(context, VideoPlayerService.class);
			playIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PLAY);
			PendingIntent playPendingIntent = PendingIntent.getService(context, REQUEST_CODE_PAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_pause, playPendingIntent);
		}
		// prev
		Intent prevIntent = new Intent(context, VideoPlayerService.class);
		prevIntent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PREV);
		PendingIntent prevPendingIntent = PendingIntent.getService(context, REQUEST_CODE_PREV, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent);

		ComponentName componentName = new ComponentName(context, LoopWidgetProvider.class);
		awm.updateAppWidget(componentName, remoteViews);
	}

	/**
	 * 
	 * @author keisuke.kobayashi
	 *
	 */
	public static class WidgetService extends Service {

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			KLog.v(TAG, "onStartCommand");
			// Register broadcast
			IntentFilter filter = new IntentFilter();
			filter.addAction(PlayerEvent.StartToLoad.getAction());
			filter.addAction(PlayerEvent.Prepared.getAction());
			filter.addAction(PlayerEvent.StateUpdate.getAction());
			registerReceiver(sReceiver, filter);
			return START_STICKY;
		}

	}

	/**
	 * Broadcast receiver to handle events from VideoPlayerService.
	 */
	private static BroadcastReceiver sReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			KLog.v(TAG, "onReceive");
			if (intent.getAction().equals(PlayerEvent.Prepared.getAction())) {
				updateRemoteViews(context, true, false);
			} else if (intent.getAction().equals(PlayerEvent.StateUpdate.getAction())) {
				boolean isPlaying = intent.getBooleanExtra("is_playing", false);
				updateRemoteViews(context, isPlaying, false);
			} else if (intent.getAction().equals(PlayerEvent.StartToLoad.getAction())) {
				updateRemoteViews(context, false, true);
			}
		}
	};
}
