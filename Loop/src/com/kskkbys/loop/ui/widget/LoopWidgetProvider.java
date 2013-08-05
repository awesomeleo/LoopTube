package com.kskkbys.loop.ui.widget;

import java.util.Date;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.service.VideoPlayerService.PlayerEvent;

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



	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		KLog.v(TAG, "onUpdate");

		Intent intent = new Intent(context.getApplicationContext(), WidgetService.class);
		context.startService(intent);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		KLog.v(TAG, "onDeleted");
	}

	/**
	 * 
	 * @author keisuke.kobayashi
	 *
	 */
	public static class WidgetService extends Service {

		@Override
		public IBinder onBind(Intent intent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {
			KLog.v(TAG, "onStartCommand");
			// Register broadcast
			IntentFilter filter = new IntentFilter();
			filter.addAction(PlayerEvent.Prepared.getAction());
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

			AppWidgetManager awm = AppWidgetManager.getInstance(context);
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
			String text = "Title: " + new Date().toString();
			remoteViews.setTextViewText(R.id.widget_title, text);
			ComponentName componentName = new ComponentName(context, LoopWidgetProvider.class);
			awm.updateAppWidget(componentName, remoteViews);
		}
	};
}
