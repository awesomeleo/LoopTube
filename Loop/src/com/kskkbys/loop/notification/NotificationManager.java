package com.kskkbys.loop.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.kskkbys.loop.MainActivity;
import com.kskkbys.loop.R;

public class NotificationManager {

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION_ID = R.string.loop_video_player_service_started;

	/**
	 * Show Now Playing notification
	 * @param context
	 * @param videoTitle
	 */
	public static void show(Context context, String videoTitle) {

		android.app.NotificationManager nm = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = "Now Playing: " + videoTitle;

		// The PendingIntent to launch our activity if the user selects this notification
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(MainActivity.FROM_NOTIFICATION, true);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		
		Notification notification = new NotificationCompat.Builder(context)
			.setContentIntent(contentIntent)
			.setSmallIcon(R.drawable.ic_stat_name)
			.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
			.setTicker(text)
			.setContentTitle(context.getText(R.string.loop_app_name))
			.setContentText(text)
			.setWhen(System.currentTimeMillis())
			.setOngoing(true)
			.getNotification();
		
		// Send the notification.
		nm.notify(NOTIFICATION_ID, notification);
	}
	
	/**
	 * Cancel the notification
	 * @param context
	 */
	public static void cancel(Context context) {
		android.app.NotificationManager nm = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}

}
